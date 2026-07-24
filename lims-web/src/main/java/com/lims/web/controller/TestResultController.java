package com.lims.web.controller;

import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.TestResult;
import com.lims.service.TestResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Test Results", description = "检测结果管理")
@RestController
@RequestMapping("/api/v1/test-results")
@RequiredArgsConstructor
public class TestResultController {

    private final TestResultService testResultService;

    @Operation(summary = "Create test result")
    @PostMapping
    @PreAuthorize("hasAnyRole('ENGINEER', 'MANAGER', 'ADMIN')")
    @AuditLog(module = "TEST_RESULT", action = "CREATE")
    public R<TestResult> create(@RequestBody TestResult result) {
        return R.ok(testResultService.create(result));
    }

    @Operation(summary = "Get results by analysis task")
    @GetMapping("/task/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public R<List<TestResult>> getByTask(@PathVariable String taskId) {
        return R.ok(testResultService.getByTaskId(taskId));
    }

    @Operation(summary = "Get results by request")
    @GetMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public R<List<TestResult>> getByRequest(@PathVariable String requestId) {
        return R.ok(testResultService.getByRequestId(requestId));
    }

    @Operation(summary = "Review test result")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @AuditLog(module = "TEST_RESULT", action = "REVIEW")
    public R<Void> review(@PathVariable String id, @RequestBody Map<String, String> body) {
        testResultService.review(id, body.get("decision"));
        return R.ok();
    }
}
