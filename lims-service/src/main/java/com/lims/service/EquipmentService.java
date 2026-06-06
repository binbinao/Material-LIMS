package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.EquipmentMapper;
import com.lims.model.entity.Equipment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentMapper equipmentMapper;

    public Page<Equipment> list(int page, int size, String status) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Equipment::getStatus, status);
        }
        wrapper.orderByAsc(Equipment::getName);
        long current = page <= 0 ? 1 : page;
        return equipmentMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public Equipment getById(String id) {
        return equipmentMapper.selectById(id);
    }

    /** Equipment status statistics */
    public Map<String, Long> getStatusStats() {
        List<Equipment> all = equipmentMapper.selectList(null);
        return all.stream().collect(Collectors.groupingBy(Equipment::getStatus, Collectors.counting()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Equipment create(Equipment entity) {
        equipmentMapper.insert(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public Equipment update(String id, Equipment entity) {
        entity.setId(id);
        equipmentMapper.updateById(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, String status) {
        Equipment entity = new Equipment();
        entity.setId(id);
        entity.setStatus(status);
        equipmentMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        equipmentMapper.deleteById(id);
    }
}
