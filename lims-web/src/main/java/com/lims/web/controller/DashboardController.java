package com.lims.web.controller;

import com.lims.common.result.R;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.*;
import com.lims.service.EquipmentService;
import com.lims.service.dashboard.CostStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.lims.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Dashboard", description = "仪表盘")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RequestMapper requestMapper;
    private final AnalysisTaskMapper taskMapper;
    private final EquipmentService equipmentService;
    private final CostStatsService costStatsService;

    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> myTasks() {
        // Issue #5: derive userId from the security context (the JWT subject
        // set by JwtAuthenticationFilter), not from a @RequestParam. The old
        // signature let any logged-in user fetch any other user's stats.
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Long> requestStats = new HashMap<>();
        for (String status : List.of("DRAFT", "SUBMITTED", "ASSIGNED", "REPORTING", "APPROVING", "COMPLETED")) {
            requestStats.put(status, requestMapper.selectCount(
                    new LambdaQueryWrapper<Request>().eq(Request::getRequesterId, userId).eq(Request::getStatus, status)));
        }
        long pendingTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<AnalysisTask>().eq(AnalysisTask::getAssigneeId, userId).eq(AnalysisTask::getStatus, "PENDING"));
        long inProgressTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<AnalysisTask>().eq(AnalysisTask::getAssigneeId, userId).eq(AnalysisTask::getStatus, "IN_PROGRESS"));
        long overdue = requestMapper.selectCount(
                new LambdaQueryWrapper<Request>().eq(Request::getRequesterId, userId).lt(Request::getDueDate, LocalDate.now())
                        .notIn(Request::getStatus, "COMPLETED", "REJECTED"));
        return R.ok(Map.of(
                "requestStats", requestStats,
                "pendingTasks", pendingTasks,
                "inProgressTasks", inProgressTasks,
                "overdue", overdue
        ));
    }

    @GetMapping("/manager-overview")
    @PreAuthorize("hasRole('MANAGER')")
    public R<Map<String, Object>> managerOverview() {
        Map<String, Long> statusCounts = new HashMap<>();
        for (String status : List.of("SUBMITTED", "ASSIGNED", "SAMPLING", "REPORTING", "APPROVING")) {
            statusCounts.put(status, requestMapper.selectCount(
                    new LambdaQueryWrapper<Request>().eq(Request::getStatus, status)));
        }
        long totalActive = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long completed = requestMapper.selectCount(
                new LambdaQueryWrapper<Request>().eq(Request::getStatus, "COMPLETED"));
        return R.ok(Map.of(
                "statusCounts", statusCounts,
                "totalActive", totalActive,
                "completed", completed
        ));
    }

    @GetMapping("/request-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public R<Map<String, Object>> requestStats(
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) String period) {
        List<Request> requests = requestMapper.selectList(
                new LambdaQueryWrapper<Request>()
                        .eq(brandId != null, Request::getBrandId, brandId)
                        .eq(typeId != null, Request::getTypeId, typeId));
        Map<String, Long> byStatus = requests.stream()
                .collect(Collectors.groupingBy(Request::getStatus, Collectors.counting()));
        Map<String, Long> byBrand = requests.stream()
                .collect(Collectors.groupingBy(Request::getBrandId, Collectors.counting()));
        return R.ok(Map.of("byStatus", byStatus, "byBrand", byBrand, "total", requests.size()));
    }

    @Operation(summary = "Cost statistics with multiple grouping dimensions: brand|type|month|item")
    @GetMapping("/cost-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public R<Map<String, Object>> costStats(
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "brand") String groupBy) {
        return R.ok(costStatsService.aggregate(brandId, typeId, startDate, endDate, groupBy));
    }

    @Operation(summary = "Export cost statistics to xlsx")
    @GetMapping("/cost-export")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<byte[]> costExport(
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "brand") String groupBy) {
        byte[] bytes = costStatsService.exportXlsx(brandId, typeId, startDate, endDate, groupBy);
        String filename = "cost-stats-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/equipment-stats")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> equipmentStats() {
        return R.ok(Map.of("statusCounts", equipmentService.getStatusStats()));
    }
}
