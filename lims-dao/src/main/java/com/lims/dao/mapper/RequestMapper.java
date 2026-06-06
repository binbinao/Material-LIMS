package com.lims.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.model.entity.Request;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequestMapper extends BaseMapper<Request> {
}
