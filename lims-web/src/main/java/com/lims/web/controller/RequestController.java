package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.common.security.SecurityUtils;
import com.lims.model.dto.AnalysisTaskAssignDTO;
import com.lims.model.dto.RequestCreateDTO;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.service.AnalysisTaskService;
import com.lims.service.RequestCommandService;
import com.lims.service.RequestQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 委托管理 Controller —— CQRS 重构后使用三个独立 Service：
 * <ul>
 *   <li>{@link RequestCommandService} — 写操作（create/submit/assign/reject/complete）</li>
 *   <li>{@link RequestQueryService} — 读操作（list/get/workflow）</li>
 *   <li>{@link AnalysisTaskService} — 任务管理（update/query tasks）</li>
 * </ul>
 */
@Tag(name = "Request Management", description = "委托管理")
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestCommandService requestCommandService;
    private final RequestQueryService requestQueryService;
    private final AnalysisTaskService analysisTaskService;

    // ---- Query endpoints (read-only) ----

    @Operation(summary = "List requests with pagination and filters")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<Request>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String keyword) {
        Page<Request> result = requestQueryService.listRequests(page, size, status, brandId, keyword);
        return R.ok(result);
    }

    @Operation(summary = "Get request by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Request> getById(@PathVariable String id) {
        return R.ok(requestQueryService.getRequest(id));
    }

    @Operation(summary = "Get analysis tasks for a request")
    @GetMapping("/{id}/tasks")
    @PreAuthorize("isAuthenticated()")
    public R<List<AnalysisTask>> getTasks(@PathVariable String id) {
        return R.ok(analysisTaskService.getAnalysisTasks(id));
    }

    @Operation(summary = "Get workflow status for a request")
    @GetMapping("/{id}/workflow")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getWorkflowStatus(@PathVariable String id) {
        return R.ok(requestQueryService.getWorkflowStatus(id));
    }

    @Operation(summary = "Get my pending workflow tasks")
    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, Object>>> myTasks() {
        return R.ok(requestQueryService.getMyPendingTasks(SecurityUtils.getCurrentUserId()));
    }

    // ---- Command endpoints (write) ----

    @Operation(summary = "Create a new request")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "CREATE")
    public R<Request> create(@Valid @RequestBody RequestCreateDTO dto) {
        Request request = requestCommandService.createRequest(dto, SecurityUtils.getCurrentUserId());
        return R.ok(request);
    }

    @Operation(summary = "Submit request for review")
    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "SUBMIT")
    public R<Void> submit(@PathVariable String id) {
        requestCommandService.submitRequest(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Manager assigns engineers to request")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "ASSIGN")
    public R<Void> assign(@PathVariable String id,
                          @RequestBody List<AnalysisTaskAssignDTO> assignments,
                          @RequestParam(required = false) String priority) {
        requestCommandService.assignRequest(id, assignments, priority);
        return R.ok();
    }

    @Operation(summary = "Manager rejects request")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "REJECT")
    public R<Void> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        requestCommandService.rejectRequest(id, body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "Receive sample for request")
    @PostMapping("/{id}/receive-sample")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ENGINEER')")
    @AuditLog(module = "REQUEST", action = "RECEIVE_SAMPLE")
    public R<Void> receiveSample(@PathVariable String id,
                                  @RequestBody Map<String, String> body) {
        requestCommandService.receiveSample(id, body.get("deliveryNote"), SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Start reporting phase")
    @PostMapping("/{id}/start-reporting")
    @PreAuthorize("hasAnyRole('ENGINEER', 'MANAGER')")
    @AuditLog(module = "REQUEST", action = "START_REPORTING")
    public R<Void> startReporting(@PathVariable String id) {
        requestCommandService.startReporting(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Manager advances request to approval phase")
    @PostMapping("/{id}/advance-to-approving")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @AuditLog(module = "REQUEST", action = "ADVANCE_TO_APPROVING")
    public R<Void> advanceToApproving(@PathVariable String id) {
        analysisTaskService.advanceToApproving(id);
        return R.ok();
    }

    @Operation(summary = "Complete request")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "COMPLETE")
    public R<Void> complete(@PathVariable String id) {
        requestCommandService.completeRequest(id);
        return R.ok();
    }

    @Operation(summary = "Update analysis task status")
    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "UPDATE_TASK")
    public R<Void> updateTask(@PathVariable String taskId,
                               @RequestBody Map<String, String> body) {
        analysisTaskService.updateAnalysisTask(taskId, body.get("status"), body.get("delayReason"),
                SecurityUtils.getCurrentUserId());
        return R.ok();
    }
}
