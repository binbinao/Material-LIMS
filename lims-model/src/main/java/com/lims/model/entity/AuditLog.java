package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class AuditLog {

    private String id;
    private String userId;
    private String module;
    private String action;
    private String entityId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
