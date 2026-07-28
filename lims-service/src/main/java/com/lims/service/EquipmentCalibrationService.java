package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.EquipmentCalibrationMapper;
import com.lims.model.entity.EquipmentCalibration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Issue #81: Equipment calibration service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentCalibrationService {

    private final EquipmentCalibrationMapper calibrationMapper;

    @Transactional(rollbackFor = Exception.class)
    public EquipmentCalibration create(EquipmentCalibration calibration) {
        calibrationMapper.insert(calibration);
        log.info("Calibration record created: equipmentId={}, date={}", calibration.getEquipmentId(), calibration.getCalibratedAt());
        return calibration;
    }

    public List<EquipmentCalibration> getByEquipmentId(String equipmentId) {
        return calibrationMapper.selectList(
                new LambdaQueryWrapper<EquipmentCalibration>()
                        .eq(EquipmentCalibration::getEquipmentId, equipmentId)
                        .orderByDesc(EquipmentCalibration::getCalibratedAt));
    }

    /**
     * Check if equipment has a valid (non-expired) calibration.
     */
    public boolean isCalibrationValid(String equipmentId) {
        List<EquipmentCalibration> records = getByEquipmentId(equipmentId);
        if (records.isEmpty()) return false;
        EquipmentCalibration latest = records.get(0);
        if (!"PASS".equals(latest.getResult())) return false;
        if (latest.getNextCalibrationDate() == null) return true;
        return latest.getNextCalibrationDate().isAfter(LocalDate.now());
    }

    /**
     * Get all equipment with expired or upcoming calibration.
     */
    public List<EquipmentCalibration> getExpiringSoon(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return calibrationMapper.selectList(
                new LambdaQueryWrapper<EquipmentCalibration>()
                        .isNotNull(EquipmentCalibration::getNextCalibrationDate)
                        .le(EquipmentCalibration::getNextCalibrationDate, threshold)
                        .ge(EquipmentCalibration::getNextCalibrationDate, LocalDate.now())
                        .orderByAsc(EquipmentCalibration::getNextCalibrationDate));
    }
}
