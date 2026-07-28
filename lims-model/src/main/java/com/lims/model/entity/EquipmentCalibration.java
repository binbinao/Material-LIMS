package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Issue #81: Equipment calibration record.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment_calibration")
public class EquipmentCalibration extends BaseEntity {

    private String equipmentId;
    private String calibrationType;
    private LocalDate calibratedAt;
    private LocalDate nextCalibrationDate;
    private String certificateNo;
    private String certificateUrl;
    private String calibratedBy;
    private String result;
    private BigDecimal rangeMin;
    private BigDecimal rangeMax;
    private String accuracy;
    private String remark;
}
