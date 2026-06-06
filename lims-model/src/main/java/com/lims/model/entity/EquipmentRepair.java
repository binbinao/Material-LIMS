package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设备维修记录。对应表 equipment_repair。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment_repair")
public class EquipmentRepair extends BaseEntity {

    private String equipmentId;
    private LocalDate reportDate;
    private String faultDescription;
    private String repairAction;
    private BigDecimal repairCost;
    private String repairedBy;
    private LocalDate completionDate;
    /** REPORTING / REPAIRING / COMPLETED */
    private String status;
}
