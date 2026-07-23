package com.lims.service;

import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
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

/**
 * 委托命令服务——负责委托生命周期中的所有写操作（CQRS Command 端）。
 * <p>
 * 拆分动机：原 RequestService 同时承担命令（create/submit/assign/reject）
 * 和查询（list/get）职责，随着业务增长，该类已接近 500 行。
 * CQRS 拆分后，写操作与读操作分离，各自独立演进。
 *
 * @see RequestQueryService 委托读操作
 * @see AnalysisTaskService 任务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestCommandService {

    private final RequestMapper requestMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisItemMapper analysisItemMapper;
    private final RequestTypeMapper requestTypeMapper;
    private final WorkflowService workflowService;
    private final HolidayService holidayService;

    /**
     * 创建委托：生成编号 → 计算到期日 → 创建分析任务 → 计算总费用
     */
    @Transactional(rollbackFor = Exception.class)
    public Request createRequest(RequestCreateDTO dto, String currentUserId) {
        Request request = new Request();

        request.setRequestNo(generateRequestNo());
        request.setBrandId(dto.getBrandId());
        request.setDeptId(dto.getDeptId());
        request.setTypeId(dto.getTypeId());
        request.setRequesterId(currentUserId);

        if (Boolean.TRUE.equals(dto.getProxyRequest())) {
            request.setProxyRequesterId(currentUserId);
            request.setRealRequesterName(dto.getRealRequesterName());
        }

        request.setPartNumber(dto.getPartNumber());
        request.setPartName(dto.getPartName());
        request.setEco(dto.getEco());
        request.setSupplierCode(dto.getSupplierCode());
        request.setSupplierName(dto.getSupplierName());

        request.setRequestReason(dto.getRequestReason());
        request.setPriority(dto.getPriority() != null ? dto.getPriority() : "NORMAL");
        request.setStatus(RequestStatus.DRAFT.getValue());

        RequestType requestType = requestTypeMapper.selectById(dto.getTypeId());
        if (requestType != null && requestType.getTaskDurationDays() != null) {
            request.setDueDate(holidayService.addBusinessDays(LocalDate.now(), requestType.getTaskDurationDays()));
        }

        requestMapper.insert(request);

        if (dto.getAnalysisItemIds() != null) {
            BigDecimal totalCost = BigDecimal.ZERO;
            int sortOrder = 0;

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

    /**
     * 提交委托：DRAFT → SUBMITTED，启动 Flowable 工作流
     */
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

        String processInstanceId = workflowService.startProcess(requestId, currentUserId);
        request.setProcessInstanceId(processInstanceId);
        requestMapper.updateById(request);

        log.info("Submitted request and started workflow: requestNo={}, processInstanceId={}",
                request.getRequestNo(), processInstanceId);
    }

    /**
     * 分配工程师 → 推进 Flowable 工作流至 sampleTask
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRequest(String requestId, List<AnalysisTaskAssignDTO> assignments, String priority) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!RequestStatus.SUBMITTED.getValue().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID);
        }

        for (AnalysisTaskAssignDTO assignment : assignments) {
            AnalysisTask task = analysisTaskMapper.selectById(assignment.getTaskId());
            if (task == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "Analysis task not found: " + assignment.getTaskId());
            }
            if (!task.getRequestId().equals(requestId)) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "Analysis task " + assignment.getTaskId() + " does not belong to request " + requestId);
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

        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), null, Map.of("decision", "assign"));
        }

        log.info("Assigned request: requestNo={}", request.getRequestNo());
    }

    /**
     * 拒绝委托：终态（COMPLETED/REJECTED）不可再拒绝。
     * 仅 MANAGER/ADMIN 可操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(String requestId, String reason) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        String current = request.getStatus();
        if (RequestStatus.COMPLETED.getValue().equals(current)
                || RequestStatus.REJECTED.getValue().equals(current)) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Request is in a terminal state (" + current + ") and cannot be rejected again");
        }
        requireRequestRole("MANAGER", "ADMIN");

        request.setStatus(RequestStatus.REJECTED.getValue());
        requestMapper.updateById(request);

        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), SecurityUtils.getCurrentUserId(),
                    Map.of("decision", "reject", "rejectReason", reason != null ? reason : ""));
        }

        log.info("Rejected request: requestNo={}, reason={}", request.getRequestNo(), reason);
    }

    /**
     * 收样：ASSIGNED → SAMPLING
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

        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), currentUserId,
                    Map.of("sampleReceived", true));
        }

        log.info("Sample received for request: requestNo={}", request.getRequestNo());
    }

    /**
     * 开始报告：SAMPLING → REPORTING
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

        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), currentUserId,
                    Map.of("reportCreated", true));
        }

        log.info("Reporting phase started for request: requestNo={}", request.getRequestNo());
    }

    /**
     * 完成委托：APPROVING → COMPLETED。仅 MANAGER/ADMIN 可操作。
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

        Map<String, Object> currentTask = workflowService.getCurrentTask(requestId);
        if (currentTask != null) {
            workflowService.completeTask((String) currentTask.get("taskId"), SecurityUtils.getCurrentUserId(),
                    Map.of("decision", "approve"));
        }

        log.info("Request completed: requestNo={}", request.getRequestNo());
    }

    /**
     * 生成委托编号：REQ-YYYY-NNNNN。编号由数据库序列原子分配，支持并发创建。
     */
    private String generateRequestNo() {
        String year = String.valueOf(LocalDate.now().getYear());
        return "REQ-" + year + "-" + String.format("%05d", requestMapper.nextRequestNumber());
    }

    /**
     * 纵深防御角色守卫——即使 Controller 遗漏 @PreAuthorize，Service 层也会拦截。
     */
    private void requireRequestRole(String... allowed) {
        for (String r : allowed) {
            if (SecurityUtils.hasRole(r)) return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "Operation requires one of roles: " + String.join(",", allowed));
    }
}