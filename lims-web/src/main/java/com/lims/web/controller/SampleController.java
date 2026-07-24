package com.lims.web.controller;

import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.Sample;
import com.lims.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Sample Management", description = "样品管理")
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @Operation(summary = "Receive a new sample")
    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ENGINEER', 'MANAGER')")
    @AuditLog(module = "SAMPLE", action = "RECEIVE")
    public R<Sample> receive(@RequestBody Sample sample) {
        return R.ok(sampleService.receive(sample));
    }

    @Operation(summary = "Get sample by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Sample> getById(@PathVariable String id) {
        return R.ok(sampleService.getById(id));
    }

    @Operation(summary = "Get samples by request")
    @GetMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public R<List<Sample>> getByRequest(@PathVariable String requestId) {
        return R.ok(sampleService.getByRequestId(requestId));
    }

    @Operation(summary = "Split a sample into a child")
    @PostMapping("/{parentSampleId}/split")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER')")
    @AuditLog(module = "SAMPLE", action = "SPLIT")
    public R<Sample> split(@PathVariable String parentSampleId, @RequestBody Sample child) {
        return R.ok(sampleService.split(parentSampleId, child));
    }

    @Operation(summary = "Dispose a sample")
    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @AuditLog(module = "SAMPLE", action = "DISPOSE")
    public R<Void> dispose(@PathVariable String id, @RequestBody Map<String, String> body) {
        sampleService.dispose(id, body.get("method"));
        return R.ok();
    }
}
