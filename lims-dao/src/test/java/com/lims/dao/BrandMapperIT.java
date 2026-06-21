package com.lims.dao;

import com.lims.dao.mapper.BrandMapper;
import com.lims.model.entity.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BrandMapperIT extends AbstractDaoIT {

    @Autowired
    BrandMapper brandMapper;

    @Test
    @DisplayName("insert then selectById returns the same row")
    void insertAndSelect() {
        Brand brand = new Brand();
        brand.setName("TestBrand-" + System.nanoTime());
        brand.setSortOrder(1);

        brandMapper.insert(brand);
        assertThat(brand.getId()).isNotBlank();

        Brand loaded = brandMapper.selectById(brand.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo(brand.getName());
        assertThat(loaded.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("logical delete sets deleted_at; selectById then returns null")
    void logicalDelete() {
        Brand brand = new Brand();
        brand.setName("ToDelete-" + System.nanoTime());
        brandMapper.insert(brand);

        brandMapper.deleteById(brand.getId());

        assertThat(brandMapper.selectById(brand.getId())).isNull();
    }
}
