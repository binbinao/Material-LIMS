package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("request_type")
public class RequestType extends BaseEntity {

    private String name;
    private Integer taskDurationDays;
    private Boolean partInfoRequired;
    private String description;
    private Integer sortOrder;
}
