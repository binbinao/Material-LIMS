package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.AnalysisItemMapper;
import com.lims.dao.mapper.AnalysisTypeMapper;
import com.lims.dao.mapper.TestGroupMapper;
import com.lims.model.entity.AnalysisItem;
import com.lims.model.entity.AnalysisType;
import com.lims.model.entity.TestGroup;
import com.lims.model.vo.AnalysisItemCascadeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisItemService {

    private static final String CACHE = "analysisItems";

    private final AnalysisItemMapper analysisItemMapper;
    private final TestGroupMapper testGroupMapper;
    private final AnalysisTypeMapper analysisTypeMapper;

    public Page<AnalysisItem> list(int page, int size, String groupId, String typeId) {
        LambdaQueryWrapper<AnalysisItem> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(AnalysisItem::getGroupId, groupId);
        }
        if (typeId != null) {
            wrapper.eq(AnalysisItem::getTypeId, typeId);
        }
        wrapper.orderByAsc(AnalysisItem::getSortOrder);
        long current = page <= 0 ? 1 : page;
        return analysisItemMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Cacheable(value = CACHE, key = "'group:' + #groupId")
    public List<AnalysisItem> listByGroup(String groupId) {
        return analysisItemMapper.selectList(
                new LambdaQueryWrapper<AnalysisItem>().eq(AnalysisItem::getGroupId, groupId).orderByAsc(AnalysisItem::getSortOrder));
    }

    /** Cascade data for frontend: Group -> Type -> Items */
    @Cacheable(value = CACHE, key = "'cascade'")
    public List<AnalysisItemCascadeVO> cascade() {
        List<TestGroup> groups = testGroupMapper.selectList(
                new LambdaQueryWrapper<TestGroup>().orderByAsc(TestGroup::getSortOrder));
        List<AnalysisType> types = analysisTypeMapper.selectList(
                new LambdaQueryWrapper<AnalysisType>().orderByAsc(AnalysisType::getSortOrder));
        List<AnalysisItem> items = analysisItemMapper.selectList(
                new LambdaQueryWrapper<AnalysisItem>().eq(AnalysisItem::getIsActive, true)
                        .orderByAsc(AnalysisItem::getSortOrder));

        Map<String, List<AnalysisType>> typesByGroup = types.stream()
                .collect(Collectors.groupingBy(AnalysisType::getGroupId));
        Map<String, List<AnalysisItem>> itemsByType = items.stream()
                .filter(i -> i.getTypeId() != null)
                .collect(Collectors.groupingBy(AnalysisItem::getTypeId));

        List<AnalysisItemCascadeVO> result = new ArrayList<>();
        for (TestGroup group : groups) {
            AnalysisItemCascadeVO groupVo = new AnalysisItemCascadeVO();
            groupVo.setId(group.getId());
            groupVo.setName(group.getName());
            List<AnalysisItemCascadeVO.TypeNode> typeNodes = new ArrayList<>();
            for (AnalysisType type : typesByGroup.getOrDefault(group.getId(), List.of())) {
                AnalysisItemCascadeVO.TypeNode typeNode = new AnalysisItemCascadeVO.TypeNode();
                typeNode.setId(type.getId());
                typeNode.setName(type.getName());
                typeNode.setItems(new ArrayList<>(itemsByType.getOrDefault(type.getId(), List.of())));
                typeNodes.add(typeNode);
            }
            groupVo.setTypes(typeNodes);
            result.add(groupVo);
        }
        return result;
    }

    public AnalysisItem getById(String id) {
        return analysisItemMapper.selectById(id);
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public AnalysisItem create(AnalysisItem entity) {
        analysisItemMapper.insert(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public AnalysisItem update(String id, AnalysisItem entity) {
        entity.setId(id);
        analysisItemMapper.updateById(entity);
        return entity;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        analysisItemMapper.deleteById(id);
    }
}
