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
import com.lims.model.entity.AnalysisItem;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.model.entity.RequestType;
import com.lims.model.enums.RequestStatus;
import com.lims.workflow.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 委托模块核心业务逻辑（RequestService）单元测试。
 *
 * <p>覆盖创建、提交、分配、驳回、收样、报告、完成、任务状态更新及查询等全部业务方法，
 * 以及每个方法的成功路径与异常/边界分支。所有外部依赖（Mapper / 工作流 / 节假日）均被 mock。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestService 委托模块测试")
class RequestServiceTest {

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private AnalysisTaskMapper analysisTaskMapper;
    @Mock
    private AnalysisItemMapper analysisItemMapper;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private RequestTypeMapper requestTypeMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private RequestService requestService;

    private static final String USER_ID = "user-001";
    private static final String REQ_ID = "req-001";

    /**
     * Set the Spring Security principal so {@code SecurityUtils.getCurrentPrincipal()}
     * returns a non-null principal with the given role set. Issue #20 made
     * auto-advance REPORTING→APPROVING require MANAGER/ADMIN; tests for that
     * path need to set the principal explicitly.
     */
    private void loginAs(String userId, String... roles) {
        var principal = new JwtTokenProvider.AuthPrincipal(
                userId, userId + "@lims.local", userId,
                String.join(",", roles), null);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                java.util.Arrays.stream(roles)
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RequestCreateDTO baseCreateDTO() {
        RequestCreateDTO dto = new RequestCreateDTO();
        dto.setBrandId("brand-1");
        dto.setDeptId("dept-1");
        dto.setTypeId("type-1");
        dto.setPartNumber("PN-001");
        dto.setPartName("Bracket");
        dto.setEco("ECO-1");
        dto.setSupplierCode("SUP-1");
        dto.setSupplierName("Supplier Inc");
        dto.setRequestReason("Quality check");
        return dto;
    }

    private Request requestWithStatus(RequestStatus status) {
        Request r = new Request();
        r.setId(REQ_ID);
        r.setRequestNo("REQ-2026-0001");
        r.setRequesterId(USER_ID);
        r.setStatus(status.getValue());
        return r;
    }

    // ----------------------------------------------------------------
    // createRequest
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("createRequest 创建委托")
    class CreateRequest {

        @Test
        @DisplayName("首个委托应生成编号 REQ-<年份>-00001 并置为 DRAFT 状态")
        void shouldGenerateFirstRequestNoAndDraftStatus() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById("type-1")).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            // Issue #36: padded to %05d so the format stays REQ-YYYY-NNNNN
            // past 9999 requests per year (4-digit %04d broke at 10000).
            String expectedNo = "REQ-" + LocalDate.now().getYear() + "-00001";
            assertThat(result.getRequestNo()).isEqualTo(expectedNo);
            assertThat(result.getStatus()).isEqualTo(RequestStatus.DRAFT.getValue());
            assertThat(result.getRequesterId()).isEqualTo(USER_ID);
            assertThat(result.getPriority()).isEqualTo("NORMAL");
            verify(requestMapper).insert(result);
        }

        @Test
        @DisplayName("已存在委托时编号应递增（5 位补零）")
        void shouldIncrementRequestNo() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(null);
            Request last = new Request();
            last.setRequestNo("REQ-" + LocalDate.now().getYear() + "-00041");
            when(requestMapper.selectOne(any())).thenReturn(last);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getRequestNo()).isEqualTo("REQ-" + LocalDate.now().getYear() + "-00042");
        }

        @Test
        @DisplayName("显式传入的优先级应被保留")
        void shouldKeepProvidedPriority() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setPriority("URGENT");
            dto.setAnalysisItemIds(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getPriority()).isEqualTo("URGENT");
        }

        @Test
        @DisplayName("代下单应记录代理人与真实委托人姓名")
        void shouldHandleProxyRequest() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setProxyRequest(true);
            dto.setRealRequesterName("Alice");
            dto.setAnalysisItemIds(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getProxyRequesterId()).isEqualTo(USER_ID);
            assertThat(result.getRealRequesterName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("非代下单不应设置代理人字段")
        void shouldNotSetProxyWhenFalse() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setProxyRequest(false);
            dto.setAnalysisItemIds(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getProxyRequesterId()).isNull();
            assertThat(result.getRealRequesterName()).isNull();
        }

        @Test
        @DisplayName("委托类型含工期时应按工作日计算截止日期")
        void shouldCalculateDueDateWhenTypeHasDuration() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(null);
            RequestType type = new RequestType();
            type.setTaskDurationDays(5);
            LocalDate due = LocalDate.now().plusDays(7);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById("type-1")).thenReturn(type);
            when(holidayService.addBusinessDays(any(LocalDate.class), eq(5))).thenReturn(due);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getDueDate()).isEqualTo(due);
            verify(holidayService).addBusinessDays(any(LocalDate.class), eq(5));
        }

        @Test
        @DisplayName("委托类型未配置工期时不应计算截止日期")
        void shouldNotCalculateDueDateWhenNoDuration() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(null);
            RequestType type = new RequestType();
            type.setTaskDurationDays(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById("type-1")).thenReturn(type);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getDueDate()).isNull();
            verify(holidayService, never()).addBusinessDays(any(), anyInt());
        }

        @Test
        @DisplayName("应为每个分析项创建任务并累加总成本")
        void shouldCreateAnalysisTasksAndSumCost() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(List.of("item-1", "item-2"));
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            AnalysisItem item1 = new AnalysisItem();
            item1.setCost(new BigDecimal("100.50"));
            AnalysisItem item2 = new AnalysisItem();
            item2.setCost(new BigDecimal("200.00"));
            when(analysisItemMapper.selectById("item-1")).thenReturn(item1);
            when(analysisItemMapper.selectById("item-2")).thenReturn(item2);

            Request result = requestService.createRequest(dto, USER_ID);

            ArgumentCaptor<AnalysisTask> taskCaptor = ArgumentCaptor.forClass(AnalysisTask.class);
            verify(analysisTaskMapper, times(2)).insert(taskCaptor.capture());
            List<AnalysisTask> tasks = taskCaptor.getAllValues();
            assertThat(tasks).extracting(AnalysisTask::getStatus).containsOnly("PENDING");
            assertThat(tasks).extracting(AnalysisTask::getSortOrder).containsExactly(0, 1);
            assertThat(result.getTotalCost()).isEqualByComparingTo("300.50");
            verify(requestMapper).updateById(result);
        }

        @Test
        @DisplayName("分析项不存在时应跳过，成本不变")
        void shouldSkipMissingAnalysisItems() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(List.of("item-1", "missing"));
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            AnalysisItem item1 = new AnalysisItem();
            item1.setCost(new BigDecimal("50"));
            when(analysisItemMapper.selectById("item-1")).thenReturn(item1);
            when(analysisItemMapper.selectById("missing")).thenReturn(null);

            Request result = requestService.createRequest(dto, USER_ID);

            verify(analysisTaskMapper, times(1)).insert(any(AnalysisTask.class));
            assertThat(result.getTotalCost()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("分析项成本为 null 时按零计算")
        void shouldTreatNullCostAsZero() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(List.of("item-1"));
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            AnalysisItem item1 = new AnalysisItem();
            item1.setCost(null);
            when(analysisItemMapper.selectById("item-1")).thenReturn(item1);

            Request result = requestService.createRequest(dto, USER_ID);

            assertThat(result.getTotalCost()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("analysisItemIds 为 null 时不应创建任务也不二次更新")
        void shouldNotCreateTasksWhenItemIdsNull() {
            RequestCreateDTO dto = baseCreateDTO();
            dto.setAnalysisItemIds(null);
            when(requestMapper.selectOne(any())).thenReturn(null);
            when(requestTypeMapper.selectById(anyString())).thenReturn(null);

            requestService.createRequest(dto, USER_ID);

            verify(analysisTaskMapper, never()).insert(any());
            verify(requestMapper, never()).updateById(any());
        }
    }

    // ----------------------------------------------------------------
    // submitRequest
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("submitRequest 提交委托")
    class SubmitRequest {

        @Test
        @DisplayName("DRAFT 状态提交成功并启动工作流")
        void shouldSubmitSuccessfully() {
            Request request = requestWithStatus(RequestStatus.DRAFT);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.startProcess(REQ_ID, USER_ID)).thenReturn("proc-1");

            requestService.submitRequest(REQ_ID, USER_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.SUBMITTED.getValue());
            assertThat(request.getSubmittedAt()).isNotNull();
            assertThat(request.getProcessInstanceId()).isEqualTo("proc-1");
            verify(requestMapper, times(2)).updateById(request);
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.submitRequest(REQ_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
            verify(workflowService, never()).startProcess(any(), any());
        }

        @Test
        @DisplayName("非 DRAFT 状态抛 REQUEST_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThatThrownBy(() -> requestService.submitRequest(REQ_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.REQUEST_STATUS_INVALID.getCode());
        }

        @Test
        @DisplayName("非本人提交抛 ACCESS_DENIED")
        void shouldThrowWhenNotRequester() {
            Request request = requestWithStatus(RequestStatus.DRAFT);
            request.setRequesterId("other-user");
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThatThrownBy(() -> requestService.submitRequest(REQ_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
            verify(workflowService, never()).startProcess(any(), any());
        }
    }

    // ----------------------------------------------------------------
    // assignRequest
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("assignRequest 分配工程师")
    class AssignRequest {

        private AnalysisTaskAssignDTO assign(String taskId, String engineerId) {
            AnalysisTaskAssignDTO a = new AnalysisTaskAssignDTO();
            a.setTaskId(taskId);
            a.setEngineerId(engineerId);
            return a;
        }

        @Test
        @DisplayName("SUBMITTED 状态分配成功并更新优先级")
        void shouldAssignSuccessfully() {
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            AnalysisTask task = new AnalysisTask();
            task.setId("task-1");
            task.setRequestId(REQ_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);

            requestService.assignRequest(REQ_ID, List.of(assign("task-1", "eng-1")), "URGENT");

            assertThat(task.getAssigneeId()).isEqualTo("eng-1");
            assertThat(request.getStatus()).isEqualTo(RequestStatus.ASSIGNED.getValue());
            assertThat(request.getPriority()).isEqualTo("URGENT");
            assertThat(request.getAssignedAt()).isNotNull();
            verify(analysisTaskMapper).updateById(task);
        }

        @Test
        @DisplayName("priority 为 null 时不修改原优先级")
        void shouldKeepPriorityWhenNull() {
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            request.setPriority("NORMAL");
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            requestService.assignRequest(REQ_ID, List.of(), null);

            assertThat(request.getPriority()).isEqualTo("NORMAL");
            assertThat(request.getStatus()).isEqualTo(RequestStatus.ASSIGNED.getValue());
        }

        @Test
        @DisplayName("任务不属于该委托时应抛 DATA_NOT_FOUND（issue #36：原为静默跳过）")
        void shouldSkipTaskFromOtherRequest() {
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            AnalysisTask task = new AnalysisTask();
            task.setId("task-1");
            task.setRequestId("other-req");
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);

            assertThatThrownBy(() ->
                    requestService.assignRequest(REQ_ID, List.of(assign("task-1", "eng-1")), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
            verify(analysisTaskMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("任务不存在时应抛 DATA_NOT_FOUND（issue #36：原为静默跳过）")
        void shouldSkipMissingTask() {
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(analysisTaskMapper.selectById("task-x")).thenReturn(null);

            assertThatThrownBy(() ->
                    requestService.assignRequest(REQ_ID, List.of(assign("task-x", "eng-1")), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
            verify(analysisTaskMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.assignRequest(REQ_ID, List.of(), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("非 SUBMITTED 状态抛 REQUEST_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            Request request = requestWithStatus(RequestStatus.DRAFT);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThatThrownBy(() -> requestService.assignRequest(REQ_ID, List.of(), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.REQUEST_STATUS_INVALID.getCode());
        }
    }

    // ----------------------------------------------------------------
    // rejectRequest
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("rejectRequest 驳回委托")
    class RejectRequest {

        @Test
        @DisplayName("驳回成功并完成工作流任务（currentUser 传 null）")
        void shouldRejectAndCompleteWorkflowTask() {
            // Review H2: rejectRequest now requires MANAGER/ADMIN role +
            // refuses terminal states. SUBMITTED + MANAGER is the happy path.
            loginAs("user-mgr", "MANAGER");
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID))
                    .thenReturn(Map.of("taskId", "t-1", "assignee", "mgr-1"));

            requestService.rejectRequest(REQ_ID, "Insufficient info");

            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED.getValue());
            verify(requestMapper).updateById(request);
            // Review M1: rejectRequest now passes the current user's id
            // (not null) so the Flowable act_hi_actinst row records the
            // rejecting user.
            verify(workflowService).completeTask(eq("t-1"), eq("user-mgr"), any());
        }

        @Test
        @DisplayName("无活动工作流任务时只更新状态")
        void shouldRejectWhenNoWorkflowTask() {
            loginAs("user-mgr", "MANAGER");
            Request request = requestWithStatus(RequestStatus.SUBMITTED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID)).thenReturn(null);

            requestService.rejectRequest(REQ_ID, "reason");

            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED.getValue());
            verify(workflowService, never()).completeTask(any(), any(), any());
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.rejectRequest(REQ_ID, "reason"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    // ----------------------------------------------------------------
    // receiveSample
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("receiveSample 收样")
    class ReceiveSample {

        @Test
        @DisplayName("ASSIGNED -> SAMPLING 成功并完成工作流任务")
        void shouldReceiveSuccessfully() {
            Request request = requestWithStatus(RequestStatus.ASSIGNED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID))
                    .thenReturn(Map.of("taskId", "t-1", "assignee", "tech-1"));

            requestService.receiveSample(REQ_ID, "DN-123", USER_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.SAMPLING.getValue());
            assertThat(request.getSampleDeliveryNote()).isEqualTo("DN-123");
            verify(workflowService).completeTask("t-1", USER_ID, Map.of("sampleReceived", true));
        }

        @Test
        @DisplayName("无工作流任务时只更新状态")
        void shouldReceiveWhenNoWorkflowTask() {
            Request request = requestWithStatus(RequestStatus.ASSIGNED);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID)).thenReturn(null);

            requestService.receiveSample(REQ_ID, "DN-1", USER_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.SAMPLING.getValue());
            verify(workflowService, never()).completeTask(any(), any(), any());
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.receiveSample(REQ_ID, "DN", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("非 ASSIGNED 状态抛 REQUEST_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            Request request = requestWithStatus(RequestStatus.SAMPLING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThatThrownBy(() -> requestService.receiveSample(REQ_ID, "DN", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.REQUEST_STATUS_INVALID.getCode());
        }
    }

    // ----------------------------------------------------------------
    // startReporting
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("startReporting 进入报告阶段")
    class StartReporting {

        @Test
        @DisplayName("SAMPLING -> REPORTING 成功")
        void shouldStartReportingSuccessfully() {
            Request request = requestWithStatus(RequestStatus.SAMPLING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID))
                    .thenReturn(Map.of("taskId", "t-1", "assignee", "eng-1"));

            requestService.startReporting(REQ_ID, USER_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.REPORTING.getValue());
            verify(workflowService).completeTask("t-1", USER_ID, Map.of("reportCreated", true));
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.startReporting(REQ_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("非 SAMPLING 状态抛 REQUEST_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            Request request = requestWithStatus(RequestStatus.REPORTING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThatThrownBy(() -> requestService.startReporting(REQ_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.REQUEST_STATUS_INVALID.getCode());
        }
    }

    // ----------------------------------------------------------------
    // completeRequest
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("completeRequest 完成委托")
    class CompleteRequest {

        @Test
        @DisplayName("完成成功并完成最终工作流任务（currentUser 传 null）")
        void shouldCompleteSuccessfully() {
            // Review H1: completeRequest now requires APPROVING + MANAGER role.
            loginAs("user-mgr", "MANAGER");
            Request request = requestWithStatus(RequestStatus.APPROVING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID))
                    .thenReturn(Map.of("taskId", "t-1", "assignee", "mgr-1"));

            requestService.completeRequest(REQ_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.COMPLETED.getValue());
            // Review M1: completeRequest now passes the current user's id
            // (not null) so the Flowable act_hi_actinst row records who
            // completed the request.
            verify(workflowService).completeTask(eq("t-1"), eq("user-mgr"), any());
        }

        @Test
        @DisplayName("无工作流任务时只更新状态")
        void shouldCompleteWhenNoWorkflowTask() {
            loginAs("user-mgr", "MANAGER");
            Request request = requestWithStatus(RequestStatus.APPROVING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);
            when(workflowService.getCurrentTask(REQ_ID)).thenReturn(null);

            requestService.completeRequest(REQ_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.COMPLETED.getValue());
            verify(workflowService, never()).completeTask(any(), any(), any());
        }

        @Test
        @DisplayName("委托不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(requestMapper.selectById(REQ_ID)).thenReturn(null);

            assertThatThrownBy(() -> requestService.completeRequest(REQ_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    // ----------------------------------------------------------------
    // updateAnalysisTask
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("updateAnalysisTask 更新分析任务状态")
    class UpdateAnalysisTask {

        private AnalysisTask task(String requestId) {
            AnalysisTask t = new AnalysisTask();
            t.setId("task-1");
            t.setRequestId(requestId);
            return t;
        }

        private AnalysisTask taskWithAssignee(String requestId, String assigneeId) {
            AnalysisTask t = task(requestId);
            t.setAssigneeId(assigneeId);
            return t;
        }

        @Test
        @DisplayName("置为 IN_PROGRESS 应记录开始时间（assignee 可调用）")
        void shouldSetStartedAt() {
            loginAs(USER_ID, "ENGINEER");
            task(REQ_ID).setAssigneeId(USER_ID);
            AnalysisTask task = taskWithAssignee(REQ_ID, USER_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);

            requestService.updateAnalysisTask("task-1", "IN_PROGRESS", null, USER_ID);

            assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(task.getStartedAt()).isNotNull();
            assertThat(task.getCompletedAt()).isNull();
            verify(analysisTaskMapper).updateById(task);
        }

        @Test
        @DisplayName("传入延期原因应被记录（assignee 可调用）")
        void shouldSetDelayReason() {
            loginAs(USER_ID, "ENGINEER");
            AnalysisTask task = taskWithAssignee(REQ_ID, USER_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);

            requestService.updateAnalysisTask("task-1", "IN_PROGRESS", "等待设备", USER_ID);

            assertThat(task.getDelayReason()).isEqualTo("等待设备");
        }

        @Test
        @DisplayName("全部任务完成且委托在 REPORTING 且调用者是 MANAGER+ 时自动转 APPROVING")
        void shouldAutoTransitionToApprovingWhenAllTasksDone() {
            // Issue #20: caller must be ADMIN/MANAGER to trigger the auto-advance.
            loginAs(USER_ID, "MANAGER");
            AnalysisTask task = taskWithAssignee(REQ_ID, USER_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);
            when(analysisTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            Request request = requestWithStatus(RequestStatus.REPORTING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            requestService.updateAnalysisTask("task-1", "COMPLETED", null, USER_ID);

            assertThat(task.getCompletedAt()).isNotNull();
            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVING.getValue());
            verify(requestMapper).updateById(request);
        }

        @Test
        @DisplayName("仍有未完成任务时不触发自动转移（即使调用者是 MANAGER）")
        void shouldNotTransitionWhenTasksPending() {
            loginAs(USER_ID, "MANAGER");
            AnalysisTask task = taskWithAssignee(REQ_ID, USER_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);
            when(analysisTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

            requestService.updateAnalysisTask("task-1", "COMPLETED", null, USER_ID);

            verify(requestMapper, never()).selectById(any());
            verify(requestMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("任务全部完成但委托不在 REPORTING 时不转移（即使调用者是 MANAGER）")
        void shouldNotTransitionWhenRequestNotReporting() {
            loginAs(USER_ID, "MANAGER");
            AnalysisTask task = taskWithAssignee(REQ_ID, USER_ID);
            when(analysisTaskMapper.selectById("task-1")).thenReturn(task);
            when(analysisTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            Request request = requestWithStatus(RequestStatus.SAMPLING);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            requestService.updateAnalysisTask("task-1", "COMPLETED", null, USER_ID);

            assertThat(request.getStatus()).isEqualTo(RequestStatus.SAMPLING.getValue());
            verify(requestMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("任务不存在抛 DATA_NOT_FOUND")
        void shouldThrowWhenTaskNotFound() {
            when(analysisTaskMapper.selectById("task-x")).thenReturn(null);

            assertThatThrownBy(() -> requestService.updateAnalysisTask("task-x", "COMPLETED", null, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
        }
    }

    // ----------------------------------------------------------------
    // 查询方法
    // ----------------------------------------------------------------
    @Nested
    @DisplayName("查询方法")
    class Queries {

        @Test
        @DisplayName("getAnalysisTasks 返回任务列表")
        void shouldGetAnalysisTasks() {
            List<AnalysisTask> tasks = List.of(new AnalysisTask());
            when(analysisTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(tasks);

            assertThat(requestService.getAnalysisTasks(REQ_ID)).isSameAs(tasks);
        }

        @Test
        @DisplayName("getWorkflowStatus 委托工作流当前任务")
        void shouldGetWorkflowStatus() {
            Map<String, Object> info = Map.of("taskId", "t-1");
            when(workflowService.getCurrentTask(REQ_ID)).thenReturn(info);

            assertThat(requestService.getWorkflowStatus(REQ_ID)).isEqualTo(info);
        }

        @Test
        @DisplayName("getMyPendingTasks 返回待办任务")
        void shouldGetMyPendingTasks() {
            List<Map<String, Object>> tasks = List.of(Map.of("taskId", "t-1"));
            when(workflowService.getPendingTasks(USER_ID)).thenReturn(tasks);

            assertThat(requestService.getMyPendingTasks(USER_ID)).isEqualTo(tasks);
        }

        @Test
        @DisplayName("getRequest 按 ID 返回委托")
        void shouldGetRequest() {
            Request request = requestWithStatus(RequestStatus.DRAFT);
            when(requestMapper.selectById(REQ_ID)).thenReturn(request);

            assertThat(requestService.getRequest(REQ_ID)).isSameAs(request);
        }

        @Test
        @DisplayName("listRequests 带全部过滤条件分页查询")
        void shouldListWithAllFilters() {
            Page<Request> page = new Page<>(1, 20);
            when(requestMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Page<Request> result = requestService.listRequests(1, 20, "DRAFT", "brand-1", "kw");

            assertThat(result).isSameAs(page);
            verify(requestMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("listRequests 无过滤条件且页码<=0时纠正为第1页")
        void shouldListWithoutFiltersAndNormalizePage() {
            Page<Request> page = new Page<>(1, 20);
            when(requestMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Page<Request> result = requestService.listRequests(0, 20, null, null, null);

            assertThat(result).isSameAs(page);
        }
    }
}
