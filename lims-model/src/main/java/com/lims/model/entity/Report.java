package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report")
public class Report extends BaseEntity {

    private String requestId;
    private String taskId;
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
}
