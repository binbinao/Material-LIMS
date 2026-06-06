package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.DepartmentMapper;
import com.lims.model.entity.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private static final String CACHE = "departments";

    private final DepartmentMapper departmentMapper;

    @Cacheable(value = CACHE, key = "'page:' + #page + ':' + #size")
    public Page<Department> list(int page, int size) {
        long current = page <= 0 ? 1 : page;
        return departmentMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getSortOrder));
    }

    @Cacheable(value = CACHE, key = "'all'")
    public List<Department> listAll() {
        return departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getLevel, Department::getSortOrder));
    }

    /** Return department tree structure (cached). */
    @Cacheable(value = CACHE, key = "'tree'")
    public List<Map<String, Object>> tree() {
        List<Department> all = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getLevel, Department::getSortOrder));
        return buildTree(all, null);
    }

    private List<Map<String, Object>> buildTree(List<Department> all, String parentId) {
        return all.stream()
                .filter(d -> (parentId == null && d.getParentId() == null) || parentId != null && parentId.equals(d.getParentId()))
                .map(d -> {
                    Map<String, Object> node = new java.util.HashMap<>();
                    node.put("id", d.getId());
                    node.put("name", d.getName());
                    node.put("parentId", d.getParentId());
                    node.put("level", d.getLevel());
                    node.put("children", buildTree(all, d.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    public Department getById(String id) {
        return departmentMapper.selectById(id);
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Department create(Department entity) {
        departmentMapper.insert(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Department update(String id, Department entity) {
        entity.setId(id);
        departmentMapper.updateById(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        departmentMapper.deleteById(id);
    }
}
