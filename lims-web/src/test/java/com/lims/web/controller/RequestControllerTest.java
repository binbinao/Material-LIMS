package com.lims.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lims.model.dto.RequestCreateDTO;
import com.lims.model.entity.AnalysisTask;
import com.lims.model.entity.Request;
import com.lims.service.RequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 委托模块 Web 层（RequestController）测试。
 *
 * <p>使用 standalone MockMvc 验证各接口的路由、参数绑定、请求体解析以及对 Service 层的委托调用。
 * 业务逻辑由 {@link RequestServiceTest} 覆盖，此处聚焦 HTTP 契约。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestController 委托接口测试")
class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(requestController)
                .setMessageConverters(converter)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/requests 分页查询并透传过滤参数")
    void shouldListRequests() throws Exception {
        when(requestService.listRequests(2, 10, "DRAFT", "brand-1", "kw"))
                .thenReturn(new Page<>(2, 10));

        mockMvc.perform(get("/api/v1/requests")
                        .param("page", "2").param("size", "10")
                        .param("status", "DRAFT").param("brandId", "brand-1").param("keyword", "kw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(requestService).listRequests(2, 10, "DRAFT", "brand-1", "kw");
    }

    @Test
    @DisplayName("GET /api/v1/requests 使用默认分页参数")
    void shouldListWithDefaults() throws Exception {
        when(requestService.listRequests(eq(1), eq(20), any(), any(), any()))
                .thenReturn(new Page<>(1, 20));

        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isOk());

        verify(requestService).listRequests(1, 20, null, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id} 返回单个委托")
    void shouldGetById() throws Exception {
        Request request = new Request();
        request.setId("req-1");
        request.setRequestNo("REQ-2026-0001");
        when(requestService.getRequest("req-1")).thenReturn(request);

        mockMvc.perform(get("/api/v1/requests/req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestNo").value("REQ-2026-0001"));
    }

    @Test
    @DisplayName("POST /api/v1/requests 创建委托并使用当前用户ID")
    void shouldCreateRequest() throws Exception {
        RequestCreateDTO dto = new RequestCreateDTO();
        dto.setBrandId("brand-1");
        dto.setTypeId("type-1");
        dto.setRequestReason("reason");
        dto.setAnalysisItemIds(List.of("item-1"));

        Request created = new Request();
        created.setId("req-1");
        when(requestService.createRequest(any(RequestCreateDTO.class), eq(USER_ID))).thenReturn(created);

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("req-1"));

        verify(requestService).createRequest(any(RequestCreateDTO.class), eq(USER_ID));
    }

    @Test
    @DisplayName("POST /api/v1/requests 缺少必填字段时校验失败")
    void shouldRejectInvalidCreate() throws Exception {
        RequestCreateDTO dto = new RequestCreateDTO();

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/requests 分析项为空数组时校验失败")
    void shouldRejectEmptyAnalysisItems() throws Exception {
        RequestCreateDTO dto = new RequestCreateDTO();
        dto.setBrandId("brand-1");
        dto.setTypeId("type-1");
        dto.setRequestReason("reason");
        dto.setAnalysisItemIds(List.of());

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/submit 提交委托")
    void shouldSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/requests/req-1/submit"))
                .andExpect(status().isOk());

        verify(requestService).submitRequest("req-1", USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/assign 分配工程师并透传优先级")
    void shouldAssign() throws Exception {
        String body = "[{\"taskId\":\"t-1\",\"engineerId\":\"e-1\"}]";

        mockMvc.perform(post("/api/v1/requests/req-1/assign")
                        .param("priority", "URGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(requestService).assignRequest(eq("req-1"), anyList(), eq("URGENT"));
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/reject 驳回并解析原因")
    void shouldReject() throws Exception {
        mockMvc.perform(post("/api/v1/requests/req-1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"not ok\"}"))
                .andExpect(status().isOk());

        verify(requestService).rejectRequest("req-1", "not ok");
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/receive-sample 收样")
    void shouldReceiveSample() throws Exception {
        mockMvc.perform(post("/api/v1/requests/req-1/receive-sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryNote\":\"DN-1\"}"))
                .andExpect(status().isOk());

        verify(requestService).receiveSample("req-1", "DN-1", USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/start-reporting 进入报告阶段")
    void shouldStartReporting() throws Exception {
        mockMvc.perform(post("/api/v1/requests/req-1/start-reporting"))
                .andExpect(status().isOk());

        verify(requestService).startReporting("req-1", USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/requests/{id}/complete 完成委托")
    void shouldComplete() throws Exception {
        mockMvc.perform(post("/api/v1/requests/req-1/complete"))
                .andExpect(status().isOk());

        verify(requestService).completeRequest("req-1");
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id}/tasks 返回分析任务列表")
    void shouldGetTasks() throws Exception {
        when(requestService.getAnalysisTasks("req-1")).thenReturn(List.of(new AnalysisTask()));

        mockMvc.perform(get("/api/v1/requests/req-1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/requests/tasks/{taskId} 更新任务状态")
    void shouldUpdateTask() throws Exception {
        mockMvc.perform(put("/api/v1/requests/tasks/task-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"delayReason\":\"late\"}"))
                .andExpect(status().isOk());

        verify(requestService).updateAnalysisTask("task-1", "COMPLETED", "late", USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id}/workflow 返回工作流状态")
    void shouldGetWorkflowStatus() throws Exception {
        when(requestService.getWorkflowStatus("req-1")).thenReturn(Map.of("taskId", "t-1"));

        mockMvc.perform(get("/api/v1/requests/req-1/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("t-1"));
    }

    @Test
    @DisplayName("GET /api/v1/requests/my-tasks 返回当前用户待办")
    void shouldGetMyTasks() throws Exception {
        when(requestService.getMyPendingTasks(USER_ID)).thenReturn(List.of(Map.of("taskId", "t-1")));

        mockMvc.perform(get("/api/v1/requests/my-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskId").value("t-1"));

        verify(requestService).getMyPendingTasks(USER_ID);
    }
}
