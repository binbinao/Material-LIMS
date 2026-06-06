package com.lims.service.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lims.common.security.JwtTokenProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String userId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createdBy", String.class, userId);
        this.strictInsertFill(metaObject, "updatedBy", String.class, userId);
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", String.class, getCurrentUserId());
    }

    /**
     * 取当前用户的 userId（DB 里 created_by/updated_by 是 VARCHAR(36)）。
     * 注意：Authentication.getName() 在 principal 不是 String/UserDetails 时会回退到 toString()，
     * 会把整个 AuthPrincipal record 序列化（>36 字符），导致写库失败 —— 必须显式取 userId 字段。
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "system";
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof JwtTokenProvider.AuthPrincipal ap) {
                return ap.userId();
            }
            if (principal instanceof String s && !"anonymousUser".equals(s)) {
                return s;
            }
        } catch (Exception ignored) {
        }
        return "system";
    }
}
