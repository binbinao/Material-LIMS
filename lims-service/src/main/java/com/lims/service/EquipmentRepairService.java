package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.EquipmentMapper;
import com.lims.dao.mapper.EquipmentRepairMapper;
import com.lims.model.entity.Equipment;
import com.lims.model.entity.EquipmentRepair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 设备维修单服务。
 *
 * <p>核心联动：创建维修单时把对应 equipment.status 置为 UNDER_REPAIR；
 * 完成维修时恢复为 ACTIVE。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentRepairService {

    public static final String STATUS_REPORTING = "REPORTING";
    public static final String STATUS_REPAIRING = "REPAIRING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String EQUIP_ACTIVE = "ACTIVE";
    public static final String EQUIP_UNDER_REPAIR = "UNDER_REPAIR";

    private final EquipmentRepairMapper repairMapper;
    private final EquipmentMapper equipmentMapper;

    public Page<EquipmentRepair> list(int page, int size, String status, String equipmentId) {
        LambdaQueryWrapper<EquipmentRepair> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(EquipmentRepair::getStatus, status);
        }
        if (equipmentId != null && !equipmentId.isBlank()) {
            wrapper.eq(EquipmentRepair::getEquipmentId, equipmentId);
        }
        wrapper.orderByDesc(EquipmentRepair::getReportDate);
        long current = page <= 0 ? 1 : page;
        return repairMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public EquipmentRepair getById(String id) {
        return repairMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentRepair create(EquipmentRepair entity) {
        if (entity.getEquipmentId() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
        Equipment equipment = equipmentMapper.selectById(entity.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (entity.getReportDate() == null) {
            entity.setReportDate(LocalDate.now());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_REPORTING);
        }
        repairMapper.insert(entity);

        // 联动：设备置为维修中（带 version 避免乐观锁失败）
        if (!EQUIP_UNDER_REPAIR.equals(equipment.getStatus())) {
            equipment.setStatus(EQUIP_UNDER_REPAIR);
            equipmentMapper.updateById(equipment);
        }
        log.info("Created repair {} for equipment {}", entity.getId(), entity.getEquipmentId());
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public EquipmentRepair update(String id, EquipmentRepair entity) {
        entity.setId(id);
        repairMapper.updateById(entity);
        return entity;
    }

    /**
     * 标记完成。把维修单 status -> COMPLETED；如果该设备没有其他未完成维修单，把设备恢复为 ACTIVE。
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(String id, String repairAction, java.math.BigDecimal repairCost, String repairedBy) {
        EquipmentRepair repair = repairMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        repair.setStatus(STATUS_COMPLETED);
        repair.setCompletionDate(LocalDate.now());
        if (repairAction != null) repair.setRepairAction(repairAction);
        if (repairCost != null) repair.setRepairCost(repairCost);
        if (repairedBy != null) repair.setRepairedBy(repairedBy);
        repairMapper.updateById(repair);

        // 该设备是否还有未完成维修单
        Long pending = repairMapper.selectCount(
                new LambdaQueryWrapper<EquipmentRepair>()
                        .eq(EquipmentRepair::getEquipmentId, repair.getEquipmentId())
                        .ne(EquipmentRepair::getStatus, STATUS_COMPLETED));
        if (pending == 0) {
            Equipment equipment = equipmentMapper.selectById(repair.getEquipmentId());
            if (equipment != null) {
                equipment.setStatus(EQUIP_ACTIVE);
                equipmentMapper.updateById(equipment);
                log.info("Equipment {} restored to ACTIVE", repair.getEquipmentId());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        repairMapper.deleteById(id);
    }
}
