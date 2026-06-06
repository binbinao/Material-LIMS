package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.common.security.SecurityUtils;
import com.lims.model.entity.Report;
import com.lims.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Report Management", description = "报告管理")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public R<Page<Report>> list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String requestId) {
        return R.ok(reportService.list(page, size, status, requestId));
    }

    @GetMapping("/{id}")
    public R<Report> getById(@PathVariable String id) {
        return R.ok(reportService.getById(id));
    }

    @GetMapping("/{id}/edit-url")
    @Operation(summary = "Get Microsoft 365 online edit URL")
    public R<String> getEditUrl(@PathVariable String id) {
        return R.ok(reportService.getEditUrl(id));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sync report content from SharePoint")
    @AuditLog(module = "REPORT", action = "SYNC")
    public R<Void> syncFromSharePoint(@PathVariable String id) {
        reportService.syncFromSharePoint(id);
        return R.ok();
    }

    @PostMapping("/requests/{requestId}/reports")
    @Operation(summary = "Create a new report for a request")
    @PreAuthorize("hasAnyRole('ENGINEER', 'MANAGER')")
    @AuditLog(module = "REPORT", action = "CREATE")
    public R<Report> create(@PathVariable String requestId) {
        return R.ok(reportService.createReport(requestId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit report for approval")
    @AuditLog(module = "REPORT", action = "SUBMIT")
    public R<Void> submit(@PathVariable String id) {
        reportService.submitReport(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve report")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REPORT", action = "APPROVE")
    public R<Void> approve(@PathVariable String id) {
        reportService.approveReport(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject report, return to engineer")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REPORT", action = "REJECT")
    public R<Void> reject(@PathVariable String id) {
        reportService.rejectReport(id);
        return R.ok();
    }

    @PostMapping("/{id}/revise")
    @Operation(summary = "Initiate report revision")
    @AuditLog(module = "REPORT", action = "REVISE")
    public R<Report> revise(@PathVariable String id, @RequestBody Map<String, String> body) {
        return R.ok(reportService.reviseReport(id, body.get("revisionNote"), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}/revisions")
    @Operation(summary = "Get report version history")
    public R<List<Report>> revisions(@PathVariable String id) {
        return R.ok(reportService.getRevisions(id));
    }
}
