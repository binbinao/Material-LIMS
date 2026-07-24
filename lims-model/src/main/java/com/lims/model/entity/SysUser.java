package com.lims.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    private String id;
    private String email;
    private String displayName;
    private String loginId;
    private String deptId;
    private String roles;
    private String externalId;

    /**
     * BCrypt-hashed password. Hidden from JSON responses — AuthService
     * is the only consumer (login / changePassword); the AuthController
     * never returns a raw SysUser over the wire without going through
     * a sanitizing layer.
     */
    @JsonIgnore
    private String passwordHash;

    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Issue #83: Security hardening fields
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private Integer sessionVersion;
}
