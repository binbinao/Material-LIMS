package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.RequestTypeMapper;
import com.lims.model.entity.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestTypeService {

    private static final String CACHE = "requestTypes";

    private final RequestTypeMapper requestTypeMapper;

    @Cacheable(value = CACHE, key = "'page:' + #page + ':' + #size")
    public Page<RequestType> list(int page, int size) {
        long current = page <= 0 ? 1 : page;
        return requestTypeMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<RequestType>().orderByAsc(RequestType::getSortOrder));
    }

    public RequestType getById(String id) {
        return requestTypeMapper.selectById(id);
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public RequestType create(RequestType entity) {
        requestTypeMapper.insert(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public RequestType update(String id, RequestType entity) {
        entity.setId(id);
        requestTypeMapper.updateById(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        requestTypeMapper.deleteById(id);
    }
}
