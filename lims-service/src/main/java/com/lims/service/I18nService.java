package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.dao.mapper.SysI18nMessageMapper;
import com.lims.model.entity.SysI18nMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * i18n 字典服务。前端启动时拉取覆盖默认值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class I18nService {

    private final SysI18nMessageMapper mapper;

    /** 取某 locale 的全部 messageKey -> messageValue。 */
    @Cacheable(value = "i18nMessages", key = "#locale")
    public Map<String, String> getMessages(String locale) {
        List<SysI18nMessage> list = mapper.selectList(
                new LambdaQueryWrapper<SysI18nMessage>().eq(SysI18nMessage::getLocale, locale));
        Map<String, String> map = new LinkedHashMap<>(list.size());
        for (SysI18nMessage m : list) {
            map.put(m.getMessageKey(), m.getMessageValue());
        }
        return map;
    }

    @CacheEvict(value = "i18nMessages", key = "#locale")
    @Transactional(rollbackFor = Exception.class)
    public void upsert(String messageKey, String locale, String messageValue) {
        SysI18nMessage existing = mapper.selectOne(
                new LambdaQueryWrapper<SysI18nMessage>()
                        .eq(SysI18nMessage::getMessageKey, messageKey)
                        .eq(SysI18nMessage::getLocale, locale));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            SysI18nMessage entity = new SysI18nMessage();
            entity.setMessageKey(messageKey);
            entity.setLocale(locale);
            entity.setMessageValue(messageValue);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            mapper.insert(entity);
        } else {
            existing.setMessageValue(messageValue);
            existing.setUpdatedAt(now);
            mapper.updateById(existing);
        }
    }

    @CacheEvict(value = "i18nMessages", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void batchUpsert(String locale, Map<String, String> messages) {
        if (messages == null || messages.isEmpty()) return;
        messages.forEach((k, v) -> upsert(k, locale, v));
    }

    @CacheEvict(value = "i18nMessages", key = "#locale")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String messageKey, String locale) {
        mapper.delete(new LambdaQueryWrapper<SysI18nMessage>()
                .eq(SysI18nMessage::getMessageKey, messageKey)
                .eq(SysI18nMessage::getLocale, locale));
    }
}
