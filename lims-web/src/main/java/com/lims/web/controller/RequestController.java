package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.common.security.SecurityUtils;
import com.lims.model.dto.AnalysisTaskAssignDTO;
import com.lims.model.dto.RequestCreateDTO;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Request Management", description = "委托管理")
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @Operation(summary = "List requests with pagination and filters")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<Request>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String keyword) {
        Page<Request> result = requestService.listRequests(page, size, status, brandId, keyword);
        return R.ok(result);
    }

    @Operation(summary = "Get request by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Request> getById(@PathVariable String id) {
        return R.ok(requestService.getRequest(id));
    }

    @Operation(summary = "Create a new request")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "CREATE")
    public R<Request> create(@Valid @RequestBody RequestCreateDTO dto) {
        Request request = requestService.createRequest(dto, SecurityUtils.getCurrentUserId());
        return R.ok(request);
    }

    @Operation(summary = "Submit request for review")
    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "SUBMIT")
    public R<Void> submit(@PathVariable String id) {
        requestService.submitRequest(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Manager assigns engineers to request")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "ASSIGN")
    public R<Void> assign(@PathVariable String id,
                          @RequestBody List<AnalysisTaskAssignDTO> assignments,
                          @RequestParam(required = false) String priority) {
        requestService.assignRequest(id, assignments, priority);
        return R.ok();
    }

    @Operation(summary = "Manager rejects request")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "REJECT")
    public R<Void> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        requestService.rejectRequest(id, body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "Receive sample for request")
    @PostMapping("/{id}/receive-sample")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ENGINEER')")
    @AuditLog(module = "REQUEST", action = "RECEIVE_SAMPLE")
    public R<Void> receiveSample(@PathVariable String id,
                                  @RequestBody Map<String, String> body) {
        requestService.receiveSample(id, body.get("deliveryNote"), SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Start reporting phase")
    @PostMapping("/{id}/start-reporting")
    @PreAuthorize("hasAnyRole('ENGINEER', 'MANAGER')")
    @AuditLog(module = "REQUEST", action = "START_REPORTING")
    public R<Void> startReporting(@PathVariable String id) {
        requestService.startReporting(id, SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Complete request")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('MANAGER')")
    @AuditLog(module = "REQUEST", action = "COMPLETE")
    public R<Void> complete(@PathVariable String id) {
        requestService.completeRequest(id);
        return R.ok();
    }

    @Operation(summary = "Get analysis tasks for a request")
    @GetMapping("/{id}/tasks")
    @PreAuthorize("isAuthenticated()")
    public R<List<AnalysisTask>> getTasks(@PathVariable String id) {
        return R.ok(requestService.getAnalysisTasks(id));
    }

    @Operation(summary = "Update analysis task status")
    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "REQUEST", action = "UPDATE_TASK")
    public R<Void> updateTask(@PathVariable String taskId,
                               @RequestBody Map<String, String> body) {
        requestService.updateAnalysisTask(taskId, body.get("status"), body.get("delayReason"), SecurityUtils.getCurrentUserId());
        return R.ok();
    }

    @Operation(summary = "Get workflow status for a request")
    @GetMapping("/{id}/workflow")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getWorkflowStatus(@PathVariable String id) {
        return R.ok(requestService.getWorkflowStatus(id));
    }

    @Operation(summary = "Get my pending workflow tasks")
    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, Object>>> myTasks() {
        return R.ok(requestService.getMyPendingTasks(SecurityUtils.getCurrentUserId()));
    }
}
