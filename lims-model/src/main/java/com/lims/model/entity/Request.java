package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("request")
public class Request extends BaseEntity {

    private String requestNo;
    private String brandId;
    private String deptId;
    private String typeId;
    private String requesterId;
    private String proxyRequesterId;
    private String realRequesterName;
    private String partNumber;
    private String partName;
    private String eco;
    private String supplierCode;
    private String supplierName;
    private String requestReason;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private String sampleDeliveryNote;
    private BigDecimal totalCost;
    private String processInstanceId;
    private LocalDateTime submittedAt;
    private LocalDateTime assignedAt;
}
