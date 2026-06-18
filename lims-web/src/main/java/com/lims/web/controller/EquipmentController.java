package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.Equipment;
import com.lims.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Equipment Management", description = "设备管理")
@RestController
@RequestMapping("/api/v1/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<Equipment>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status) {
        return R.ok(equipmentService.list(page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Equipment> getById(@PathVariable String id) {
        return R.ok(equipmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "EQUIPMENT", action = "CREATE")
    public R<Equipment> create(@Valid @RequestBody Equipment entity) {
        return R.ok(equipmentService.create(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "EQUIPMENT", action = "UPDATE")
    public R<Equipment> update(@PathVariable String id, @Valid @RequestBody Equipment entity) {
        return R.ok(equipmentService.update(id, entity));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "EQUIPMENT", action = "UPDATE_STATUS")
    public R<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        equipmentService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "EQUIPMENT", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        equipmentService.delete(id);
        return R.ok();
    }
}
