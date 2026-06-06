package com.lims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.dao.mapper.BrandMapper;
import com.lims.model.entity.Brand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private static final String CACHE = "brands";

    private final BrandMapper brandMapper;

    @Cacheable(value = CACHE, key = "'page:' + #page + ':' + #size")
    public Page<Brand> listBrands(int page, int size) {
        long current = page <= 0 ? 1 : page;
        Page<Brand> pageParam = new Page<>(current, size);
        return brandMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getSortOrder));
    }

    /** Full list (no paging) for dropdowns; cached aggressively. */
    @Cacheable(value = CACHE, key = "'all'")
    public List<Brand> listAll() {
        return brandMapper.selectList(
                new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getSortOrder));
    }

    public Brand getBrand(String id) {
        return brandMapper.selectById(id);
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Brand createBrand(Brand brand) {
        brandMapper.insert(brand);
        return brand;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Brand updateBrand(String id, Brand brand) {
        brand.setId(id);
        brandMapper.updateById(brand);
        return brand;
    }

    @CacheEvict(value = CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteBrand(String id) {
        brandMapper.deleteById(id);
    }
}
