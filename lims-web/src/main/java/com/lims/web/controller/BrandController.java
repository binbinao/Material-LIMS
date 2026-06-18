package com.lims.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lims.common.annotation.AuditLog;
import com.lims.common.result.R;
import com.lims.model.entity.Brand;
import com.lims.dao.mapper.BrandMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Brand Management", description = "品牌管理")
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandMapper brandMapper;

    @Operation(summary = "List brands with pagination")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<Page<Brand>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        long current = page <= 0 ? 1 : page;
        Page<Brand> pageParam = new Page<>(current, size);
        Page<Brand> result = brandMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Brand>().orderByAsc(Brand::getSortOrder));
        return R.ok(result);
    }

    @Operation(summary = "Get brand by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Brand> getById(@PathVariable String id) {
        return R.ok(brandMapper.selectById(id));
    }

    @Operation(summary = "Create brand")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "BRAND", action = "CREATE")
    public R<Brand> create(@Valid @RequestBody Brand brand) {
        brandMapper.insert(brand);
        return R.ok(brand);
    }

    @Operation(summary = "Update brand")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "BRAND", action = "UPDATE")
    public R<Brand> update(@PathVariable String id, @Valid @RequestBody Brand brand) {
        brand.setId(id);
        brandMapper.updateById(brand);
        return R.ok(brand);
    }

    @Operation(summary = "Delete brand")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "BRAND", action = "DELETE")
    public R<Void> delete(@PathVariable String id) {
        brandMapper.deleteById(id);
        return R.ok();
    }
}
