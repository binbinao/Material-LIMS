package com.lims.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lims.common.security.JwtTokenProvider;
import com.lims.dao.mapper.AuditLogMapper;
import com.lims.model.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志 AOP。
 *
 * <p>触发：方法上标 {@link com.lims.common.annotation.AuditLog} 时。
 * 写入 {@code sys_operation_log}：module/action/userId/entityId(URI 末段)/ip/detail。
 *
 * <p>detail 字段保存请求参数 JSON（自动跳过 MultipartFile / HttpServletRequest 等不可序列化类型；
 * 字符串字段截断到 4000 字符以内）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final int DETAIL_MAX_LEN = 4000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditLogAnn)")
    public Object around(ProceedingJoinPoint joinPoint, com.lims.common.annotation.AuditLog auditLogAnn) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            AuditLog entity = new AuditLog();
            entity.setModule(auditLogAnn.module());
            entity.setAction(auditLogAnn.action());

            entity.setUserId(currentUserId());

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entity.setIp(extractIp(request));
                String uri = request.getRequestURI();
                String[] parts = uri.split("/");
                if (parts.length > 0) {
                    String last = parts[parts.length - 1];
                    // skip when last segment is the resource name not an id (e.g. /brands)
                    if (last.length() >= 16 || last.matches("\\d+")) {
                        entity.setEntityId(last);
                    }
                }
            }

            entity.setDetail(serializeArgs(joinPoint));
            entity.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("Failed to record audit log", e);
        }

        return result;
    }

    /** 取 userId 字符串；若 principal 是 AuthPrincipal record 则取其 userId 字段，避免 toString 越界。 */
    private String currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof JwtTokenProvider.AuthPrincipal ap) return ap.userId();
            if (principal instanceof String s && !"anonymousUser".equals(s)) return s;
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            int comma = ip.indexOf(',');
            if (comma > 0) ip = ip.substring(0, comma);
        }
        return ip;
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) return null;

            Map<String, Object> snapshot = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a == null) continue;
                if (a instanceof MultipartFile mf) {
                    snapshot.put(paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i,
                            Map.of("file", mf.getOriginalFilename(), "size", mf.getSize()));
                } else if (a instanceof HttpServletRequest
                        || a.getClass().getName().startsWith("org.springframework.web")) {
                    // skip framework objects
                } else {
                    snapshot.put(paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i, a);
                }
            }
            if (snapshot.isEmpty()) return null;
            String json = OBJECT_MAPPER.writeValueAsString(snapshot);
            if (json.length() > DETAIL_MAX_LEN) {
                json = json.substring(0, DETAIL_MAX_LEN - 3) + "...";
            }
            return json;
        } catch (Exception e) {
            return "{\"_serializeError\":\"" + e.getClass().getSimpleName() + "\"}";
        }
    }
}
