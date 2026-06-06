package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_group")
public class TestGroup extends BaseEntity {

    private String name;
    private String description;
    private Integer sortOrder;
}
