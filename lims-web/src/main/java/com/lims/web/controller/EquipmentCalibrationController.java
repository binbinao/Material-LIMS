package com.lims.web.controller;

import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.EquipmentCalibration;
import com.lims.service.EquipmentCalibrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Equipment Calibration", description = "设备校准管理")
@RestController
@RequestMapping("/api/v1/equipment-calibrations")
@RequiredArgsConstructor
public class EquipmentCalibrationController {

    private final EquipmentCalibrationService calibrationService;

    @Operation(summary = "Create calibration record")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @AuditLog(module = "CALIBRATION", action = "CREATE")
    public R<EquipmentCalibration> create(@RequestBody EquipmentCalibration calibration) {
        return R.ok(calibrationService.create(calibration));
    }

    @Operation(summary = "Get calibration history for equipment")
    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    public R<List<EquipmentCalibration>> getByEquipment(@PathVariable String equipmentId) {
        return R.ok(calibrationService.getByEquipmentId(equipmentId));
    }

    @Operation(summary = "Check if equipment calibration is valid")
    @GetMapping("/equipment/{equipmentId}/valid")
    @PreAuthorize("isAuthenticated()")
    public R<Boolean> isCalibrationValid(@PathVariable String equipmentId) {
        return R.ok(calibrationService.isCalibrationValid(equipmentId));
    }

    @Operation(summary = "Get calibrations expiring soon")
    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public R<List<EquipmentCalibration>> getExpiring(@RequestParam(defaultValue = "30") int days) {
        return R.ok(calibrationService.getExpiringSoon(days));
    }
}
