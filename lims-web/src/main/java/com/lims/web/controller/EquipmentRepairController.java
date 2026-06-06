package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.EquipmentRepair;
import com.lims.service.EquipmentRepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "Equipment Repair", description = "设备维修管理")
@RestController
@RequestMapping("/api/v1/equipment-repairs")
@RequiredArgsConstructor
public class EquipmentRepairController {

    private final EquipmentRepairService service;

    @GetMapping
    public R<Page<EquipmentRepair>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String equipmentId) {
        return R.ok(service.list(page, size, status, equipmentId));
    }

    @GetMapping("/{id}")
    public R<EquipmentRepair> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "Create repair record (auto sets equipment.status=UNDER_REPAIR)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @AuditLog(module = "EQUIPMENT_REPAIR", action = "CREATE")
    public R<EquipmentRepair> create(@Valid @RequestBody EquipmentRepair entity) {
        return R.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @AuditLog(module = "EQUIPMENT_REPAIR", action = "UPDATE")
    public R<EquipmentRepair> update(@PathVariable String id, @Valid @RequestBody EquipmentRepair entity) {
        return R.ok(service.update(id, entity));
    }

    @Operation(summary = "Mark repair completed; equipment auto restored to ACTIVE if no more pending repairs")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    @AuditLog(module = "EQUIPMENT_REPAIR", action = "COMPLETE")
    public R<Void> complete(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String repairAction = body == null ? null : (String) body.get("repairAction");
        BigDecimal repairCost = body == null || body.get("repairCost") == null
                ? null : new BigDecimal(String.valueOf(body.get("repairCost")));
        String repairedBy = body == null ? null : (String) body.get("repairedBy");
        service.complete(id, repairAction, repairCost, repairedBy);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "EQUIPMENT_REPAIR", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }
}
