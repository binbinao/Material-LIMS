package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * i18n 字典持久化条目。对应表 sys_i18n_message。
 *
 * <p>该表无 deleted_at / version 字段，因此不继承 BaseEntity。
 */
@Data
@TableName("sys_i18n_message")
public class SysI18nMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String messageKey;
    private String locale;
    private String messageValue;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
