package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report")
public class Report extends BaseEntity {

    /**
     * Override BaseEntity's {@code @TableId(ASSIGN_UUID)} with INPUT so
     * ReportService.createReport() can stamp a human-readable
     * "rpt-NNN" id before insert instead of a random UUID. The
     * companion {@code report_id_rpt_prefix_chk} CHECK constraint
     * (V10 migration) catches regressions at the DB layer.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String requestId;
    private String authorId;
    private String versionNumber;
    private String revisionNote;
    private String status;
    private String fileUrl;
    private String pdfUrl;
    private String sharepointFileId;
    private String sharepointEditUrl;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private LocalDateTime submittedAt;

    // Issue #82: E-signature fields (21 CFR Part 11)
    private String signatureUserId;
    private String signatureMeaning;
    private LocalDateTime signedAt;
}
