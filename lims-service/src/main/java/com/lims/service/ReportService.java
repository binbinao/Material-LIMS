package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.dao.mapper.AnalysisTaskMapper;
import com.lims.dao.mapper.ReportMapper;
import com.lims.dao.mapper.RequestMapper;
import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import com.lims.model.enums.ReportStatus;
import com.lims.model.enums.RequestStatus;
import com.lims.service.report.ReportTemplateService;
import com.lims.service.report.WordToPdfConverter;
import com.lims.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportMapper reportMapper;
    @SuppressWarnings("unused")
    private final AnalysisTaskMapper analysisTaskMapper;
    private final RequestMapper requestMapper;
    private final ReportTemplateService reportTemplateService;
    private final WordToPdfConverter wordToPdfConverter;
    private final FileStorageService fileStorageService;

    public Page<Report> list(int page, int size, String status, String requestId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Report::getStatus, status);
        if (requestId != null) wrapper.eq(Report::getRequestId, requestId);
        wrapper.orderByDesc(Report::getCreatedAt);
        long current = page <= 0 ? 1 : page;
        return reportMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public Report getById(String id) {
        return reportMapper.selectById(id);
    }

    public List<Report> getRevisions(String reportId) {
        // Issue #60 (P1): the caller passes a report id, but the
        // conceptual query is "all reports that have ever been generated
        // for this request" — i.e. all rows whose request_id matches the
        // parent report's request_id. The original implementation
        // filtered by report.id (== only the current row), hiding the
        // revision history.
        Report current = reportMapper.selectById(reportId);
        if (current == null) return List.of();
        return reportMapper.selectList(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getRequestId, current.getRequestId())
                        .orderByDesc(Report::getVersionNumber));
    }

    /**
     * Create a new report for a request: render docx template → upload → store URL.
     */
    @Transactional(rollbackFor = Exception.class)
    public Report createReport(String requestId, String authorId) {
        // Issue #58 (P5): a report is only meaningful once the request has
        // results worth reporting. Reject when the parent request is still
        // DRAFT/SUBMITTED/ASSIGNED/SAMPLING.
        Request parent = requestMapper.selectById(requestId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "Request not found: " + requestId);
        }
        String parentStatus = parent.getStatus();
        if (!RequestStatus.REPORTING.getValue().equals(parentStatus)
                && !RequestStatus.APPROVING.getValue().equals(parentStatus)
                && !RequestStatus.COMPLETED.getValue().equals(parentStatus)) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID,
                    "Request not in a reportable state (must be REPORTING, APPROVING, or COMPLETED)");
        }

        Report report = new Report();
        report.setRequestId(requestId);
        report.setAuthorId(authorId);
        report.setVersionNumber("V1.0");
        report.setStatus(ReportStatus.DRAFT.getValue());
        reportMapper.insert(report);

        try {
            String reportNo = "RPT-" + report.getId();
            Path docx = reportTemplateService.generate(requestId, reportNo, report.getVersionNumber(), null);
            String docxUrl = fileStorageService.upload(docx, "reports/" + requestId);
            report.setFileUrl(docxUrl);

            Path pdf = wordToPdfConverter.convert(docx);
            if (pdf != null) {
                String pdfUrl = fileStorageService.upload(pdf, "reports/" + requestId);
                report.setPdfUrl(pdfUrl);
            }
            reportMapper.updateById(report);
        } catch (Exception e) {
            log.warn("Report file generation failed (record kept): {}", e.getMessage());
        }

        log.info("Created report: requestId={}, reportId={}", requestId, report.getId());
        return report;
    }

    /**
     * Submit report for approval
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(String reportId, String authorId) {
        Report report = reportMapper.selectById(reportId);
        validateReportOwnership(report, reportId, authorId);

        if (!ReportStatus.DRAFT.getValue().equals(report.getStatus())
                && !ReportStatus.REVISING.getValue().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REPORT_NOT_EDITABLE);
        }

        report.setStatus(ReportStatus.IN_REVIEW.getValue());
        report.setSubmittedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        log.info("Submitted report for approval: reportId={}", reportId);
    }

    /**
     * Approve report
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveReport(String reportId, String managerId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        if (!ReportStatus.IN_REVIEW.getValue().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REPORT_NOT_EDITABLE);
        }
        // Issue #52 (P4): four-eyes principle. The author may not approve
        // their own report — even if they also hold a MANAGER/ADMIN role.
        // Without this guard, the dev user (and any manager who authors a
        // report) could self-approve, bypassing the review step entirely.
        if (report.getAuthorId() != null && report.getAuthorId().equals(managerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Approver must not be the report author");
        }

        report.setStatus(ReportStatus.APPROVED.getValue());
        report.setApprovedBy(managerId);
        report.setApprovedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        log.info("Approved report: reportId={}, approvedBy={}", reportId, managerId);
    }

    /**
     * Reject report (return to engineer for revision)
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(String reportId, String managerId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);

        report.setStatus(ReportStatus.REVISING.getValue());
        report.setRejectedBy(managerId);
        report.setRejectedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        log.info("Rejected report, returned for revision: reportId={}, rejectedBy={}",
                reportId, managerId);
    }

    /**
     * Revise report — create new version (Major+1, Minor reset to 0)
     */
    @Transactional(rollbackFor = Exception.class)
    public Report reviseReport(String reportId, String revisionNote, String userId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        if (!ReportStatus.APPROVED.getValue().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REQUEST_STATUS_INVALID, "Only approved reports can be revised");
        }
        if (revisionNote == null || revisionNote.isBlank()) {
            throw new BusinessException(ErrorCode.REVISION_NOTE_REQUIRED);
        }

        String newVersion = incrementVersion(report.getVersionNumber());
        report.setVersionNumber(newVersion);
        report.setRevisionNote(revisionNote);
        report.setStatus(ReportStatus.REVISING.getValue());
        report.setApprovedBy(null);
        report.setApprovedAt(null);
        reportMapper.updateById(report);

        // Regenerate docx with revision note
        try {
            String reportNo = "RPT-" + report.getId();
            Path docx = reportTemplateService.generate(report.getRequestId(), reportNo, newVersion, revisionNote);
            String docxUrl = fileStorageService.upload(docx, "reports/" + report.getRequestId());
            report.setFileUrl(docxUrl);
            Path pdf = wordToPdfConverter.convert(docx);
            if (pdf != null) {
                report.setPdfUrl(fileStorageService.upload(pdf, "reports/" + report.getRequestId()));
            }
            reportMapper.updateById(report);
        } catch (Exception e) {
            log.warn("Revise re-generation failed: {}", e.getMessage());
        }

        log.info("Revised report: reportId={}, newVersion={}", reportId, newVersion);
        return report;
    }

    /**
     * Get M365 online edit URL
     */
    public String getEditUrl(String reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        return report.getSharepointEditUrl();
    }

    /**
     * Sync report content from SharePoint (placeholder until SharePoint is wired)
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncFromSharePoint(String reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        log.info("Sync from SharePoint requested: reportId={} (no-op until SharePoint integration is enabled)",
                reportId);
    }

    private void validateReportOwnership(Report report, String reportId, String userId) {
        if (report == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        if (!report.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || !currentVersion.startsWith("V")) return "V1.0";
        String numPart = currentVersion.substring(1);
        try {
            int major = Integer.parseInt(numPart.split("\\.")[0]);
            return "V" + (major + 1) + ".0";
        } catch (NumberFormatException e) {
            return "V1.0";
        }
    }
}
