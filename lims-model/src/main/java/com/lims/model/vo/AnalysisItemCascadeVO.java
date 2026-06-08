package com.lims.model.vo;

import com.lims.model.entity.AnalysisItem;
import lombok.Data;

import java.util.List;

/**
 * 分析项目级联选择数据：测试分组 -> 分析类型 -> 分析项目。
 *
 * <p>前端 RequestCreate 的 Cascader 依赖此三层结构(group.name -> type.name -> item)。
 */
@Data
public class AnalysisItemCascadeVO {

    private String id;
    private String name;
    private List<TypeNode> types;

    @Data
    public static class TypeNode {
        private String id;
        private String name;
        private List<AnalysisItem> items;
    }
}
