package com.lims.workflow;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String PROCESS_DEFINITION_KEY = "requestProcess";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * Start a new request workflow process.
     * 受 Resilience4j 熔断器保护：Flowable 引擎异常时重试 2 次后熔断。
     */
    @CircuitBreaker(name = "workflow")
    @Retry(name = "workflow")
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String requestId, String requesterId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", requestId);
        variables.put("requesterId", requesterId);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY, requestId, variables);

        log.info("Started workflow process: processInstanceId={}, requestId={}", instance.getId(), requestId);
        return instance.getId();
    }

    /**
     * Complete the current task and move to next step.
     * <p>Lookup order:
     * 1. task assigned to userId
     * 2. task where userId is a candidate
     * 3. task exists by ID alone (ADMIN / system bypass – dev profile or manager-level override)
     * <p>受 Resilience4j 熔断器保护：Flowable API 异常时重试 2 次后熔断。
     */
    @CircuitBreaker(name = "workflow")
    @Retry(name = "workflow")
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, String userId, Map<String, Object> variables) {
        Task task = null;
        if (userId != null) {
            task = taskService.createTaskQuery().taskId(taskId).taskAssignee(userId).singleResult();
            if (task == null) {
                task = taskService.createTaskQuery().taskId(taskId).taskCandidateUser(userId).singleResult();
            }
        }
        if (task == null) {
            // ADMIN / system bypass: task exists but belongs to another user (e.g. dev-user-0001 acting on behalf)
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        }
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        taskService.complete(taskId, variables);
        log.info("Completed task: taskId={}, processInstanceId={}, byUser={}", taskId, task.getProcessInstanceId(), userId);
    }

    /**
     * Get pending tasks for a user. Returns empty list if userId is null or not recognized by Flowable.
     */
    public List<Map<String, Object>> getPendingTasks(String userId) {
        if (userId == null || userId.isBlank()) {
            return java.util.Collections.emptyList();
        }
        List<Task> tasks;
        try {
            tasks = taskService.createTaskQuery()
                    .taskCandidateOrAssigned(userId)
                    .orderByTaskCreateTime().desc()
                    .list();
        } catch (Exception e) {
            log.warn("Flowable getPendingTasks failed for userId={}: {}", userId, e.getMessage());
            return java.util.Collections.emptyList();
        }

        return tasks.stream().map(task -> {
            Map<String, Object> info = new HashMap<>();
            info.put("taskId", task.getId());
            info.put("taskName", task.getName());
            info.put("processInstanceId", task.getProcessInstanceId());
            info.put("createTime", task.getCreateTime());

            // Get process variables
            Map<String, Object> processVariables = runtimeService.getVariables(task.getProcessInstanceId());
            info.put("requestId", processVariables.get("requestId"));
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * Get current task for a request
     */
    public Map<String, Object> getCurrentTask(String requestId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(requestId)
                .singleResult();

        if (instance == null) {
            return null;
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        if (task == null) {
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("taskId", task.getId());
        info.put("taskName", task.getName());
        info.put("assignee", task.getAssignee());
        return info;
    }

    /**
     * Check if process is completed
     */
    public boolean isProcessCompleted(String requestId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(requestId)
                .singleResult();
        return instance == null; // If no active instance, process is completed
    }

    // =============================================
    // 一致性校验与修复
    // =============================================

    /**
     * 表示 Flowable 进程当前所处的 BPMN 任务节点。
     */
    public enum ProcessStage {
        ASSIGN,     // assignTask — 等待经理分配
        SAMPLING,   // sampleTask — 等待收样
        REPORTING,  // reportTask — 等待创建报告
        APPROVING,  // approveTask — 等待审批
        COMPLETED,  // 无活跃进程
        REJECTED,   // 无活跃进程（reject 分支）
        UNKNOWN     // 进程存在但当前任务未知
    }

    /**
     * 校验 Flowable 进程阶段与业务状态是否一致。
     *
     * @return null = 一致；非 null = 不一致描述
     */
    public String verifyConsistency(String requestId, String businessStatus) {
        ProcessStage flowableStage = getCurrentStage(requestId);
        if (flowableStage == null) {
            return "Flowable 进程查询失败";
        }

        ProcessStage expected = mapBusinessStatusToStage(businessStatus);
        if (expected != flowableStage) {
            return String.format(
                    "状态不一致：Flowable 进程在 [%s] 阶段，但业务状态为 [%s]（期望 [%s]）",
                    flowableStage, businessStatus, expected);
        }
        return null; // 一致
    }

    /**
     * 获取指定 requestId 对应的 BPMN 进程阶段。
     */
    public ProcessStage getCurrentStage(String requestId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(requestId)
                .singleResult();

        // 无活跃进程 → COMPLETED 或 REJECTED（取决于业务状态，这里返回 COMPLETED 作为终点）
        if (instance == null) {
            return ProcessStage.COMPLETED;
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .singleResult();

        if (task == null) {
            return ProcessStage.UNKNOWN;
        }

        return switch (task.getTaskDefinitionKey()) {
            case "assignTask" -> ProcessStage.ASSIGN;
            case "sampleTask" -> ProcessStage.SAMPLING;
            case "reportTask" -> ProcessStage.REPORTING;
            case "approveTask" -> ProcessStage.APPROVING;
            default -> ProcessStage.UNKNOWN;
        };
    }

    /**
     * 将业务状态码映射为期望的 BPMN 阶段。
     */
    private ProcessStage mapBusinessStatusToStage(String businessStatus) {
        return switch (businessStatus) {
            case "DRAFT", "SUBMITTED" -> ProcessStage.ASSIGN;
            case "ASSIGNED" -> ProcessStage.SAMPLING;
            case "SAMPLING" -> ProcessStage.REPORTING;
            case "REPORTING" -> ProcessStage.APPROVING;
            case "APPROVING" -> ProcessStage.APPROVING;
            case "COMPLETED", "REJECTED" -> ProcessStage.COMPLETED;
            default -> ProcessStage.UNKNOWN;
        };
    }
}
