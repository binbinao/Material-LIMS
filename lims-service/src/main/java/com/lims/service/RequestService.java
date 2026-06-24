package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.JwtTokenProvider;
import com.lims.common.security.SecurityUtils;
import com.lims.dao.mapper.*;
import com.lims.model.dto.AnalysisTaskAssignDTO;
import com.lims.model.dto.RequestCreateDTO;
import com.lims.model.entity.*;
import com.lims.model.enums.RequestStatus;
import com.lims.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {

    private final RequestMapper requestMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisItemMapper analysisItemMapper;
    private final BrandMapper brandMapper;
    private final RequestTypeMapper requestTypeMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkflowService workflowService;
    private final HolidayService holidayService;

    @Transactional(rollbackFor = Exception.class)
    public Request createRequest(RequestCreateDTO dto, String currentUserId) {
        Request request = new Request();

        // Generate request number: REQ-YYYY-NNNN
        request.setRequestNo(generateRequestNo());

        request.setBrandId(dto.getBrandId());
        request.setDeptId(dto.getDeptId());
        request.setTypeId(dto.getTypeId());
        request.setRequesterId(currentUserId);

        // Handle proxy request
        if (Boolean.TRUE.equals(dto.getProxyRequest())) {
            request.setProxyRequesterId(currentUserId);
            request.setRealRequesterName(dto.getRealRequesterName());
        }

        // Part & Supplier info
        request.setPartNumber(dto.getPartNumber());
        request.setPartName(dto.getPartName());
        request.setEco(dto.getEco());
        request.setSupplierCode(dto.getSupplierCode());
        request.setSupplierName(dto.getSupplierName());

        request.setRequestReason(dto.getRequestReason());
        request.setPriority(dto.getPriority() != null ? dto.getPriority() : "NORMAL");
        request.setStatus(RequestStatus.DRAFT.getValue());

        // Calculate due date via HolidayService (skips weekends + national/company holidays, cached per year)
        RequestType requestType = requestTypeMapper.selectById(dto.getTypeId());
        if (requestType != null && requestType.getTaskDurationDays() != null) {
            request.setDueDate(holidayService.addBusinessDays(LocalDate.now(), requestType.getTaskDurationDays()));
        }

        requestMapper.insert(request);

        // Create analysis tasks
        if (dto.getAnalysisItemIds() != null) {
            BigDecimal totalCost = BigDecimal.ZERO;
            int sortOrder = 0;
            
            // ✅ 修复 N+1 查询：批量查询所有分析项
            List<AnalysisItem> items = analysisItemMapper.selectBatchIds(dto.getAnalysisItemIds());
            Map<String, AnalysisItem> itemMap = items.stream()
                    .collect(java.util.stream.Collectors.toMap(AnalysisItem::getId, item -> item));
            
            for (String itemId : dto.getAnalysisItemIds()) {
                AnalysisItem item = itemMap.get(itemId);
                if (item != null) {
                    AnalysisTask task = new AnalysisTask();
                    task.setRequestId(request.getId());
                    task.setItemId(itemId);
                    task.setStatus("PENDING");
                    task.setSortOrder(sortOrder++);
                    analysisTaskMapper.insert(task);

                    if (item.getCost() != null) {
                        totalCost = totalCost.add(item.getCost());
                    }
                }
            }
            request.setTotalCost(totalCost);
            requestMapper.updateById(request);
        }

        log.info("Created request: requestNo={}, requesterId={}", request.getRequestNo(), currentUserId);
        return request;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitRequest(String requestId, String currentUserId) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.DRAFT.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID);
        }
        if (!request.getRequesterId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        request.setStatus(RequestStatus.SUBMITTED.getValue());
        request.setSubmittedAt(LocalDateTime.now());
        requestMapper.updateById(request);

        // Start Flowable workflow process
        String processInstanceId = workflowService.startProcess(requestId, currentUserId);
        request.setProcessInstanceId(processInstanceId);
        requestMapper.updateById(request);

        log.info("Submitted request and started workflow: requestNo={}, processInstanceId={}", request.getRequestNo(), processInstanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRequest(String requestId, List<AnalysisTaskAssignDTO> assignments, String priority) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.SUBMITTED.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID);
        }

        // Assign engineers to analysis tasks. Issue #36 L-7: validate
        // that each taskId actually belongs to the request — silently
        // skipping wrong-task DTOs was confusing for the API client.
        for (AnalysisTaskAssignDTO assignment : assignments) {
            AnalysisTask task = analysisTaskMapper.selectById(assignment.getTaskId());
            if (task == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "Analysis task not found: " + assignment.getTaskId());
            }
            if (!task.getRequestId().equals(requestId)) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "Analysis task " + assignment.getTaskId()
                                + " does not belong to request " + requestId);
            }
            task.setAssigneeId(assignment.getEngineerId());
            analysisTaskMapper.updateById(task);
        }

        if (priority != null) {
            request.setPriority(priority);
        }
        request.setStatus(RequestStatus.ASSIGNED.getValue());
        request.setAssignedAt(LocalDateTime.now());
        requestMapper.updateById(request);

        // Advance Flowable workflow: complete "assignTask" with decision=assign so the process
        // moves past managerDecision to sampleTask.
        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), null, Map.of("decision", "assign"));
        }

        log.info("Assigned request: requestNo={}", request.getRequestNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(String requestId, String reason) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        // Issue (review H2): previously had no state guard, so a MANAGER
        // could POST /reject on a COMPLETED request and re-flip it to
        // REJECTED. Terminal states (COMPLETED, REJECTED) are no longer
        // re-stateable.
        String current = request.getStatus();
        if (RequestStatus.COMPLETED.getValue().equals(current)
                || RequestStatus.REJECTED.getValue().equals(current)) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Request is in a terminal state (" + current + ") and cannot be rejected again");
        }
        requireRequestRole("MANAGER", "ADMIN");

        request.setStatus(RequestStatus.REJECTED.getValue());
        requestMapper.updateById(request);

        // Complete workflow task with decision=reject. We pass the current
        // user so the Flowable act_hi_actinst row records who actually
        // rejected (review M1).
        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), SecurityUtils.getCurrentUserId(),
                    Map.of("decision", "reject", "rejectReason", reason != null ? reason : ""));
        }

        log.info("Rejected request: requestNo={}, reason={}", request.getRequestNo(), reason);
    }

    /**
     * Receive sample - transition from ASSIGNED to SAMPLING
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveSample(String requestId, String deliveryNote, String currentUserId) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.ASSIGNED.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID);
        }

        request.setStatus(RequestStatus.SAMPLING.getValue());
        request.setSampleDeliveryNote(deliveryNote);
        requestMapper.updateById(request);

        // Complete the "Sample Receive" task in workflow
        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), currentUserId, Map.of("sampleReceived", true));
        }

        log.info("Sample received for request: requestNo={}", request.getRequestNo());
    }

    /**
     * Start reporting phase - transition from SAMPLING to REPORTING
     */
    @Transactional(rollbackFor = Exception.class)
    public void startReporting(String requestId, String currentUserId) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.SAMPLING.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID);
        }

        request.setStatus(RequestStatus.REPORTING.getValue());
        requestMapper.updateById(request);

        // Complete the "Create Report" task in workflow
        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), currentUserId, Map.of("reportCreated", true));
        }

        log.info("Reporting phase started for request: requestNo={}", request.getRequestNo());
    }

    /**
     * Advance a request to APPROVING state. Issue #20: only MANAGER/ADMIN
     * may trigger this auto-advance. The assignee (ENGINEER/TECHNICIAN)
     * finishing their own task must NOT silently flip the request into
     * manager-review state — that bypasses the manager's manual review
     * step in the BPMN flow. The request stays in REPORTING until a
     * manager explicitly approves.
     */
    private void advanceToApproval(Request request) {
        if (request == null || !RequestStatus.REPORTING.getValue().equals(request.getStatus())) {
            return;
        }
        JwtTokenProvider.AuthPrincipal principal = SecurityUtils.getCurrentPrincipal();
        if (principal == null
                || (!principal.hasRole("ADMIN") && !principal.hasRole("MANAGER"))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only MANAGER/ADMIN may advance a request to APPROVING");
        }
        request.setStatus(RequestStatus.APPROVING.getValue());
        requestMapper.updateById(request);
    }

    /**
     * Complete request - transition from APPROVING to COMPLETED.
     *
     * Issue (review H1): previously this method had no state guard, so a
     * MANAGER could call POST /complete on a DRAFT/SUBMITTED/REJECTED
     * request and silently flip it to COMPLETED, bypassing assign →
     * receive-sample → start-reporting → approve entirely. The state
     * check and the service-layer role guard mirror approveReport so
     * the controller's @PreAuthorize cannot be the only line of defense.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeRequest(String requestId) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.APPROVING.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Request must be APPROVING before it can be completed (current=" + request.getStatus() + ")");
        }
        requireRequestRole("MANAGER", "ADMIN");

        request.setStatus(RequestStatus.COMPLETED.getValue());
        requestMapper.updateById(request);

        // Complete the final task in workflow with decision=approve
        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), SecurityUtils.getCurrentUserId(), Map.of("decision", "approve"));
        }

        log.info("Request completed: requestNo={}", request.getRequestNo());
    }

    /**
     * Update analysis task status
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAnalysisTask(String taskId, String status, String delayReason, String currentUserId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        // Issue #15: ownership check. Only the assignee (or a MANAGER /
        // ADMIN) may mutate the task. Without this any logged-in user
        // can mark someone else's task as COMPLETED, which auto-
        // transitions the request into APPROVING — an attacker can
        // bypass the manager-approval step entirely.
        JwtTokenProvider.AuthPrincipal principal = SecurityUtils.getCurrentPrincipal();
        boolean callerIsManagerOrAdmin = principal != null
                && (principal.hasRole("ADMIN") || principal.hasRole("MANAGER"));
        if (!callerIsManagerOrAdmin
                && (task.getAssigneeId() == null
                        || !task.getAssigneeId().equals(currentUserId))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only the task's assignee (or a MANAGER) may update this task");
        }

        task.setStatus(status);
        if ("IN_PROGRESS".equals(status)) {
            task.setStartedAt(LocalDateTime.now());
        } else if ("COMPLETED".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        if (delayReason != null) {
            task.setDelayReason(delayReason);
        }
        analysisTaskMapper.updateById(task);

        // Check if all tasks for this request are completed
        if ("COMPLETED".equals(status)) {
            long pendingCount = analysisTaskMapper.selectCount(
                    new LambdaQueryWrapper<AnalysisTask>()
                            .eq(AnalysisTask::getRequestId, task.getRequestId())
                            .ne(AnalysisTask::getStatus, "COMPLETED"));
            if (pendingCount == 0) {
                // Issue #20: only MANAGER/ADMIN may auto-advance to APPROVING.
                // The role gate lives in advanceToApproval — non-managers
                // finishing their own task must NOT silently flip the request
                // into manager-review state. See advanceToApproval Javadoc.
                Request request = requestMapper.selectById(task.getRequestId());
                advanceToApproval(request);
            }
        }

        log.info("Updated analysis task: taskId={}, status={}", taskId, status);
    }

    /**
     * Get analysis tasks for a request
     */
    public List<AnalysisTask> getAnalysisTasks(String requestId) {
        return analysisTaskMapper.selectList(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getRequestId, requestId)
                        .orderByAsc(AnalysisTask::getSortOrder));
    }

    /**
     * Get workflow current task info for a request
     */
    public Map<String, Object> getWorkflowStatus(String requestId) {
        return workflowService.getCurrentTask(requestId);
    }

    /**
     * Get pending workflow tasks for a user
     */
    public List<Map<String, Object>> getMyPendingTasks(String userId) {
        return workflowService.getPendingTasks(userId);
    }

    public Page<Request> listRequests(int page, int size, String status, String brandId, String keyword) {
        long current = page <= 0 ? 1 : page;
        Page<Request> pageParam = new Page<>(current, size);
        LambdaQueryWrapper<Request> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Request::getStatus, status);
        }
        if (brandId != null) {
            wrapper.eq(Request::getBrandId, brandId);
        }
        if (keyword != null) {
            wrapper.and(w -> w
                    .like(Request::getRequestNo, keyword)
                    .or().like(Request::getPartNumber, keyword)
                    .or().like(Request::getPartName, keyword));
        }
        wrapper.orderByDesc(Request::getCreatedAt);

        return requestMapper.selectPage(pageParam, wrapper);
    }

    public Request getRequest(String requestId) {
        return requestMapper.selectById(requestId);
    }

    private String generateRequestNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "REQ-" + year + "-";

        // Get the max request number for this year
        LambdaQueryWrapper<Request> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Request::getRequestNo, prefix)
                .orderByDesc(Request::getRequestNo)
                .last("LIMIT 1");
        Request lastRequest = requestMapper.selectOne(wrapper);

        int nextNum = 1;
        if (lastRequest != null) {
            String lastNo = lastRequest.getRequestNo();
            String numPart = lastNo.substring(prefix.length());
            nextNum = Integer.parseInt(numPart) + 1;
        }

        return prefix + String.format("%05d", nextNum);
    }

    /**
     * Defense-in-depth role guard for state-transitioning actions on
     * Request (complete / reject). Reads roles from the SecurityContext
     * and refuses if none of {@code allowed} matches. Mirrors the
     * {@code requireRoleAny} helper in ReportService so that a future
     * controller that forgets its @PreAuthorize still cannot bypass the
     * rule (review M6 noted the inconsistency with submitRequest, which
     * still has no service-level role check — by design, since submit
     * is keyed on ownership rather than role).
     */
    private void requireRequestRole(String... allowed) {
        for (String r : allowed) {
            if (SecurityUtils.hasRole(r)) return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "Operation requires one of roles: " + String.join(",", allowed));
    }
}
