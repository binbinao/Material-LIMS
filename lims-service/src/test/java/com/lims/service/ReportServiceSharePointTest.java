package com.lims.service;

import com.lims.dao.mapper.ReportMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import com.lims.service.report.ReportTemplateService;
import com.lims.service.report.WordToPdfConverter;
import com.lims.service.sharepoint.SharePointClient;
import com.lims.service.sharepoint.SharePointException;
import com.lims.service.sharepoint.SharePointPathResolver;
import com.lims.service.sharepoint.SharePointProperties;
import com.lims.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportServiceSharePointTest {

    private ReportMapper reportMapper;
    private RequestMapper requestMapper;
    private ReportTemplateService tpl;
    private WordToPdfConverter conv;
    private FileStorageService storage;
    private SharePointClient client;
    private ReportService svc;

    @BeforeEach
    void setUp() {
        reportMapper = mock(ReportMapper.class);
        requestMapper = mock(RequestMapper.class);
        tpl = mock(ReportTemplateService.class);
        conv = mock(WordToPdfConverter.class);
        storage = mock(FileStorageService.class);
        client = mock(SharePointClient.class);
        when(client.isEnabled()).thenReturn(true);

        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "", "/sites/lims", "Documents", "Reports", "", "?action=edit");
        SharePointPathResolver resolver = new SharePointPathResolver(props);

        svc = new ReportService(reportMapper, null, requestMapper, tpl, conv, storage,
            client, resolver);
        ReflectionTestUtils.setField(svc, "demoEnabled", true);
    }

    private Request parent(String status) {
        Request r = new Request();
        r.setId("req-1");
        r.setRequestNo("REQ-2026-0001");
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.of(2026, 1, 15, 0, 0));
        return r;
    }

    @Test
    void createReport_sharePointSuccess_setsFileIdAndEditUrl() throws Exception {
        when(requestMapper.selectById("req-1")).thenReturn(parent("REPORTING"));
        when(reportMapper.nextReportNumber()).thenReturn(1L);
        when(tpl.generate(any(), any(), any(), any())).thenReturn(java.nio.file.Files.createTempFile("r", ".docx"));
        when(storage.upload(any(), anyString())).thenReturn("minio:reports/req-1/r.docx");
        when(conv.convert(any())).thenReturn(null);
        when(client.uploadDocx(anyString(), any()))
            .thenReturn(new SharePointClient.SharePointUploadResult("item-9", "https://example.com/x?action=edit"));

        Report created = svc.createReport("req-1", "user-1");

        assertThat(created.getSharepointFileId()).isEqualTo("item-9");
        assertThat(created.getSharepointEditUrl()).isEqualTo("https://example.com/x?action=edit");
    }

    @Test
    void createReport_sharePointThrows_keepsLocalRecordButEmptyMeta() throws Exception {
        when(requestMapper.selectById("req-1")).thenReturn(parent("REPORTING"));
        when(reportMapper.nextReportNumber()).thenReturn(2L);
        when(tpl.generate(any(), any(), any(), any())).thenReturn(java.nio.file.Files.createTempFile("r", ".docx"));
        when(storage.upload(any(), anyString())).thenReturn("minio:reports/req-1/r.docx");
        when(client.uploadDocx(anyString(), any())).thenThrow(new SharePointException("Graph 401"));

        Report created = svc.createReport("req-1", "user-1");

        assertThat(created.getSharepointFileId()).isNull();
        assertThat(created.getSharepointEditUrl()).isNull();
    }

    @Test
    void syncFromSharePoint_noFileId_isNoOp() {
        Report r = new Report();
        r.setId("rpt-1");
        r.setRequestId("req-1");
        r.setSharepointFileId(null);
        when(reportMapper.selectById("rpt-1")).thenReturn(r);
        svc.syncFromSharePoint("rpt-1");
        verify(client, never()).downloadDocx(anyString());
    }
}