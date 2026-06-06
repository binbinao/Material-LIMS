package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.Department;
import com.lims.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Department Management", description = "部门管理")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public R<?> list(@RequestParam(required = false) Integer page,
                     @RequestParam(required = false, defaultValue = "20") Integer size) {
        if (page != null) {
            return R.ok(departmentService.list(page, size));
        }
        // no page param → return flat list (for tree building on frontend)
        return R.ok(departmentService.listAll());
    }

    @GetMapping("/tree")
    @Operation(summary = "Get department tree structure")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(departmentService.tree());
    }

    @GetMapping("/{id}")
    public R<Department> getById(@PathVariable String id) {
        return R.ok(departmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "DEPARTMENT", action = "CREATE")
    public R<Department> create(@Valid @RequestBody Department entity) {
        return R.ok(departmentService.create(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "DEPARTMENT", action = "UPDATE")
    public R<Department> update(@PathVariable String id, @Valid @RequestBody Department entity) {
        return R.ok(departmentService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "DEPARTMENT", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        departmentService.delete(id);
        return R.ok();
    }
}
