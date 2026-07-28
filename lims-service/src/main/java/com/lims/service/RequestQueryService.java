package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 委托查询服务——负责委托生命周期中的所有读操作（CQRS Query 端）。
 * <p>
 * 拆分动机：将查询逻辑从原 RequestService 中解耦，与写操作分离。
 * 查询服务天然读优化友好（如缓存、只读事务），与命令服务的强一致性需求不同。
 *
 * @see RequestCommandService 委托写操作
 * @see AnalysisTaskService 任务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestQueryService {

    private final RequestMapper requestMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final WorkflowService workflowService;

    /**
     * 分页查询委托列表，支持按状态、品牌、关键词筛选
     */
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

    /**
     * 根据 ID 查询委托详情
     */
    public Request getRequest(String requestId) {
        Request request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        return request;
    }

    /**
     * 获取委托的分析任务列表
     * <p>
     * 委托给 {@link AnalysisTaskService} 以保持任务查询的单一入口。
     */
    public List<AnalysisTask> getAnalysisTasks(String requestId) {
        return analysisTaskMapper.selectList(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getRequestId, requestId)
                        .orderByAsc(AnalysisTask::getSortOrder));
    }

    /**
     * 获取委托的 Flowable 工作流当前任务信息
     */
    public Map<String, Object> getWorkflowStatus(String requestId) {
        return workflowService.getCurrentTask(requestId);
    }

    /**
     * 获取用户的待办工作流任务列表
     */
    public List<Map<String, Object>> getMyPendingTasks(String userId) {
        return workflowService.getPendingTasks(userId);
    }
}