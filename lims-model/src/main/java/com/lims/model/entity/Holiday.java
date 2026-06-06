package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("holiday")
public class Holiday extends BaseEntity {

    private LocalDate date;
    private String name;
    private String type;
    private Integer year;
}
