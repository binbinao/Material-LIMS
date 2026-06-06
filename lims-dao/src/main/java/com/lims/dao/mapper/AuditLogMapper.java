package com.lims.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lims.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
