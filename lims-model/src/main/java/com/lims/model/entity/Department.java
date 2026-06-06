package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("department")
public class Department extends BaseEntity {

    private String name;
    private String parentId;
    private String externalId;
    private Integer level;
    private Integer sortOrder;
}
