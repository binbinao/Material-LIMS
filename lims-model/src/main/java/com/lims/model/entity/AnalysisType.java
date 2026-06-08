package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("analysis_type")
public class AnalysisType extends BaseEntity {

    private String groupId;
    private String name;
    private String description;
    private Integer sortOrder;
}
