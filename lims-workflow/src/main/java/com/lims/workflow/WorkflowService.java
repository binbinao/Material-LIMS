package com.lims.workflow;

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
     * Start a new request workflow process
     */
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
     * Complete the current task and move to next step
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, String userId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).taskAssignee(userId).singleResult();
        if (task == null) {
            // Try candidate group
            task = taskService.createTaskQuery().taskId(taskId).taskCandidateUser(userId).singleResult();
        }
        if (task == null) {
            throw new RuntimeException("Task not found or not assigned to user: " + taskId);
        }

        taskService.complete(taskId, variables);
        log.info("Completed task: taskId={}, processInstanceId={}", taskId, task.getProcessInstanceId());
    }

    /**
     * Get pending tasks for a user
     */
    public List<Map<String, Object>> getPendingTasks(String userId) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .list();

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
}
