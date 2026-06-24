package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.JwtTokenProvider;
import com.lims.common.security.SecurityUtils;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.model.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析任务服务——从 RequestService 拆分出的任务管理职责。
 * <p>
 * 拆分动机：RequestService 同时管理委托生命周期和分析任务生命周期，
 * 违反了单一职责原则。将任务状态转换、任务查询提取到独立服务中。
 *
 * @see RequestCommandService 委托写操作
 * @see RequestQueryService 委托读操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;
    private final RequestMapper requestMapper;

    /**
     * 更新分析任务状态，包含权限校验、状态转换和自动推进逻辑。
     * <p>
     * 所有权校验：只有任务分配人（或 MANAGER/ADMIN）可以修改任务。
     * 当所有任务完成后，自动将委托推进至 APPROVING 状态（仅 MANAGER/ADMIN 可触发）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAnalysisTask(String taskId, String status, String delayReason, String currentUserId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

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

        if ("COMPLETED".equals(status)) {
            long pendingCount = analysisTaskMapper.selectCount(
                    new LambdaQueryWrapper<AnalysisTask>()
                            .eq(AnalysisTask::getRequestId, task.getRequestId())
                            .ne(AnalysisTask::getStatus, "COMPLETED"));
            if (pendingCount == 0) {
                Request request = requestMapper.selectById(task.getRequestId());
                advanceToApproval(request);
            }
        }

        log.info("Updated analysis task: taskId={}, status={}", taskId, status);
    }

    /**
     * 获取委托的所有分析任务
     */
    public List<AnalysisTask> getAnalysisTasks(String requestId) {
        return analysisTaskMapper.selectList(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getRequestId, requestId)
                        .orderByAsc(AnalysisTask::getSortOrder));
    }

    /**
     * 将委托推进至 APPROVING 状态，仅 MANAGER/ADMIN 可触发。
     * 防止工程师完成任务后绕过经理审批直接进入审批阶段。
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
}