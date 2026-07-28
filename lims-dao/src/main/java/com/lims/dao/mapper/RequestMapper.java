package com.lims.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.model.entity.Request;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequestMapper extends BaseMapper<Request> {

    @Select("SELECT nextval('request_no_seq')")
    long nextRequestNumber();
}
