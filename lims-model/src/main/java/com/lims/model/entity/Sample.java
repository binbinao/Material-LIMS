package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Issue #80: Sample entity with barcode and full lifecycle.
 * The sample table existed since V1 but had no Java entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sample")
public class Sample extends BaseEntity {

    private String requestId;
    private String sampleName;
    private String barcode;
    private String batchNo;
    private String container;
    private BigDecimal quantity;
    private String quantityUnit;
    private String storageLocation;
    private String custodianId;
    private String receivedCondition;
    private String sampleStatus;
    private LocalDateTime receivedAt;
    private LocalDateTime disposedAt;
    private String disposalMethod;
    private String parentSampleId;
}
