package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment")
public class Equipment extends BaseEntity {

    private String name;
    private String model;
    private String serialNumber;
    private String status;
    private String location;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String description;
}
