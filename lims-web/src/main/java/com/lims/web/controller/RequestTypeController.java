package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.RequestType;
import com.lims.service.RequestTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Request Type Management", description = "委托类型管理")
@RestController
@RequestMapping("/api/v1/request-types")
@RequiredArgsConstructor
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    @GetMapping
    public R<Page<RequestType>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return R.ok(requestTypeService.list(page, size));
    }

    @GetMapping("/{id}")
    public R<RequestType> getById(@PathVariable String id) {
        return R.ok(requestTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "REQUEST_TYPE", action = "CREATE")
    public R<RequestType> create(@Valid @RequestBody RequestType entity) {
        return R.ok(requestTypeService.create(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "REQUEST_TYPE", action = "UPDATE")
    public R<RequestType> update(@PathVariable String id, @Valid @RequestBody RequestType entity) {
        return R.ok(requestTypeService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "REQUEST_TYPE", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        requestTypeService.delete(id);
        return R.ok();
    }
}
