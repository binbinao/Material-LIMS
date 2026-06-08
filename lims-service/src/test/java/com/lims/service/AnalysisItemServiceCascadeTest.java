package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lims.dao.mapper.AnalysisItemMapper;
import com.lims.dao.mapper.AnalysisTypeMapper;
import com.lims.dao.mapper.TestGroupMapper;
import com.lims.model.entity.AnalysisItem;
import com.lims.model.entity.AnalysisType;
import com.lims.model.entity.TestGroup;
import com.lims.model.vo.AnalysisItemCascadeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisItemService.cascade 三层级联")
class AnalysisItemServiceCascadeTest {

    @Mock
    AnalysisItemMapper analysisItemMapper;
    @Mock
    TestGroupMapper testGroupMapper;
    @Mock
    AnalysisTypeMapper analysisTypeMapper;

    @InjectMocks
    AnalysisItemService service;

    private static TestGroup group(String id, String name) {
        TestGroup g = new TestGroup();
        g.setId(id);
        g.setName(name);
        return g;
    }

    private static AnalysisType type(String id, String groupId, String name) {
        AnalysisType t = new AnalysisType();
        t.setId(id);
        t.setGroupId(groupId);
        t.setName(name);
        return t;
    }

    private static AnalysisItem item(String id, String groupId, String typeId, String name) {
        AnalysisItem i = new AnalysisItem();
        i.setId(id);
        i.setGroupId(groupId);
        i.setTypeId(typeId);
        i.setName(name);
        return i;
    }

    @Test
    @DisplayName("按 group->type->item 三层嵌套,并带出分组与类型名称")
    void buildsThreeLevelTree() {
        when(testGroupMapper.selectList(any())).thenReturn(List.of(
                group("g1", "Mechanical"), group("g2", "Chemical")));
        when(analysisTypeMapper.selectList(any())).thenReturn(List.of(
                type("t1", "g1", "Tensile"),
                type("t2", "g1", "Hardness"),
                type("t3", "g2", "Composition")));
        when(analysisItemMapper.selectList((Wrapper<AnalysisItem>) any())).thenReturn(List.of(
                item("i1", "g1", "t1", "Tensile Strength"),
                item("i2", "g1", "t2", "Rockwell"),
                item("i3", "g2", "t3", "ICP")));

        List<AnalysisItemCascadeVO> tree = service.cascade();

        assertThat(tree).hasSize(2);
        AnalysisItemCascadeVO g1 = tree.stream().filter(g -> g.getId().equals("g1")).findFirst().orElseThrow();
        assertThat(g1.getName()).isEqualTo("Mechanical");
        assertThat(g1.getTypes()).hasSize(2);

        AnalysisItemCascadeVO.TypeNode t1 = g1.getTypes().stream()
                .filter(t -> t.getId().equals("t1")).findFirst().orElseThrow();
        assertThat(t1.getName()).isEqualTo("Tensile");
        assertThat(t1.getItems()).extracting(AnalysisItem::getId).containsExactly("i1");

        AnalysisItemCascadeVO g2 = tree.stream().filter(g -> g.getId().equals("g2")).findFirst().orElseThrow();
        assertThat(g2.getTypes()).hasSize(1);
        assertThat(g2.getTypes().get(0).getItems()).extracting(AnalysisItem::getName).containsExactly("ICP");
    }

    @Test
    @DisplayName("不含分析项的类型与分组也保留(空 items 列表)")
    void keepsEmptyBranches() {
        when(testGroupMapper.selectList(any())).thenReturn(List.of(group("g1", "Mechanical")));
        when(analysisTypeMapper.selectList(any())).thenReturn(List.of(type("t1", "g1", "Tensile")));
        when(analysisItemMapper.selectList((Wrapper<AnalysisItem>) any())).thenReturn(List.of());

        List<AnalysisItemCascadeVO> tree = service.cascade();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getTypes()).hasSize(1);
        assertThat(tree.get(0).getTypes().get(0).getItems()).isEmpty();
    }
}
