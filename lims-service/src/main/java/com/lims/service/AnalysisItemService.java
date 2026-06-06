package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.AnalysisItemMapper;
import com.lims.model.entity.AnalysisItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisItemService {

    private static final String CACHE = "analysisItems";

    private final AnalysisItemMapper analysisItemMapper;

    public Page<AnalysisItem> list(int page, int size, String groupId, String typeId) {
        LambdaQueryWrapper<AnalysisItem> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(AnalysisItem::getGroupId, groupId);
        }
        if (typeId != null) {
            wrapper.eq(AnalysisItem::getTypeId, typeId);
        }
        wrapper.orderByAsc(AnalysisItem::getSortOrder);
        long current = page <= 0 ? 1 : page;
        return analysisItemMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Cacheable(value = CACHE, key = "'group:' + #groupId")
    public List<AnalysisItem> listByGroup(String groupId) {
        return analysisItemMapper.selectList(
                new LambdaQueryWrapper<AnalysisItem>().eq(AnalysisItem::getGroupId, groupId).orderByAsc(AnalysisItem::getSortOrder));
    }

    /** Cascade data for frontend: Group -> Type -> Items */
    @Cacheable(value = CACHE, key = "'cascade'")
    public List<AnalysisItem> cascade() {
        return analysisItemMapper.selectList(
                new LambdaQueryWrapper<AnalysisItem>().eq(AnalysisItem::getIsActive, true).orderByAsc(AnalysisItem::getSortOrder));
    }

    public AnalysisItem getById(String id) {
        return analysisItemMapper.selectById(id);
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public AnalysisItem create(AnalysisItem entity) {
        analysisItemMapper.insert(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public AnalysisItem update(String id, AnalysisItem entity) {
        entity.setId(id);
        analysisItemMapper.updateById(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        analysisItemMapper.deleteById(id);
    }
}
