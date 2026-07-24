package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Issue #79: Structured test result with specification judgment.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_result")
public class TestResult extends BaseEntity {

    private String analysisTaskId;
    private String requestId;
    private String itemId;
    private String testMethod;
    private String equipmentId;
    private String rawValue;
    private BigDecimal enteredValue;
    private String unit;
    private BigDecimal specLower;
    private BigDecimal specUpper;
    private String judgment;
    private BigDecimal uncertainty;
    private Integer repeatCount;
    private String resultAttachmentUrl;
    private String remark;
    private String enteredBy;
    private LocalDateTime enteredAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String status;
}
