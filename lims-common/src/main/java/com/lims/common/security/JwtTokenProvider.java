package com.lims.common.security;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * LIMS 自签 JWT 工具：HS256 签名，TTL 默认 8 小时。
 * Claims:
 * - sub        : userId
 * - email      : 用户邮箱
 * - displayName: 显示名
 * - roles      : 逗号分隔的角色字符串（与 sys_user.roles 一致）
 * - deptId     : 部门 ID
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_DEPT_ID = "deptId";

    @Value("${security.jwt.secret}")
    private String secret;

    /**
     * Minimum HS256 secret length in bytes. Refuse to boot if the configured
     * secret is shorter — a 32-byte threshold is the RFC 7518 §3.2 minimum for
     * HS256 and matches what the previous hard-coded default provided.
     */
    private static final int MIN_SECRET_BYTES = 32;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.secret is not configured. Set the JWT_SECRET " +
                            "environment variable (or property) before starting the application.");
        }
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret is too short (" + bytes + " bytes). " +
                            "HS256 requires at least " + MIN_SECRET_BYTES + " bytes. " +
                            "Set JWT_SECRET to a stronger value before starting the application.");
        }
    }

    /** TTL（小时），默认 8 小时（与工作日对齐） */
    @Value("${security.jwt.ttl-hours:8}")
    private long ttlHours;

    public String generate(String userId,
                           String email,
                           String displayName,
                           String roles,
                           String deptId) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofHours(ttlHours));

        JWT jwt = JWT.create()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiresAt(Date.from(exp))
                .setPayload(CLAIM_EMAIL, email)
                .setPayload(CLAIM_DISPLAY_NAME, displayName)
                .setPayload(CLAIM_ROLES, roles == null ? "" : roles)
                .setPayload(CLAIM_DEPT_ID, deptId);

        return jwt.setSigner(JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8))).sign();
    }

    /**
     * 解析并验证 token。失败返回 null。
     */
    public AuthPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            if (!JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            JWT jwt = JWTUtil.parseToken(token);
            // 过期检查
            Object expObj = jwt.getPayload("exp");
            if (expObj instanceof Number expEpoch) {
                if (expEpoch.longValue() < Instant.now().getEpochSecond()) {
                    return null;
                }
            }
            String userId = (String) jwt.getPayload("sub");
            String email = (String) jwt.getPayload(CLAIM_EMAIL);
            String displayName = (String) jwt.getPayload(CLAIM_DISPLAY_NAME);
            String roles = (String) jwt.getPayload(CLAIM_ROLES);
            String deptId = (String) jwt.getPayload(CLAIM_DEPT_ID);
            return new AuthPrincipal(userId, email, displayName, roles, deptId);
        } catch (Exception e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    public long getTtlHours() {
        return ttlHours;
    }

    /** JWT 解析后的当前用户主体 */
    public record AuthPrincipal(String userId,
                                String email,
                                String displayName,
                                String roles,
                                String deptId) {

        public boolean hasRole(String role) {
            if (roles == null || roles.isBlank()) return false;
            for (String r : roles.split(",")) {
                if (r.trim().equalsIgnoreCase(role)) return true;
            }
            return false;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "userId", userId == null ? "" : userId,
                    "email", email == null ? "" : email,
                    "displayName", displayName == null ? "" : displayName,
                    "roles", roles == null ? "" : roles,
                    "deptId", deptId == null ? "" : deptId
            );
        }
    }
}
