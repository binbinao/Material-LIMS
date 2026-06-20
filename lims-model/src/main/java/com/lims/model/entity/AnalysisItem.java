package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("analysis_item")
public class AnalysisItem extends BaseEntity {

    private String groupId;
    private String siteId;
    private String typeId;
    private String name;
    private String code;
    private String equipmentId;
    private String testStandards;
    private String specificationId;
    private BigDecimal cost;
    private BigDecimal unitPrice;
    private String unit;
    private String description;
    private String attachmentUrl;
    private Boolean isActive;
    private Integer sortOrder;
}
