package com.lims.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当前登录用户工具。
 * 在 dev profile 下若未登录，可由 SecurityConfig 注入虚拟 ADMIN 用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 获取当前登录用户 ID（即 sys_user.id），未登录返回 null */
    public static String getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtTokenProvider.AuthPrincipal p) {
            return p.userId();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        // getName() 对非 String/UserDetails principal 会回退到 toString()，不能写入 VARCHAR(36)
        String userId = extractUserIdFromPrincipal(principal);
        if (userId != null) {
            return userId;
        }
        return null;
    }

    private static String extractUserIdFromPrincipal(Object principal) {
        if (principal == null) return null;
        try {
            var method = principal.getClass().getMethod("userId");
            Object value = method.invoke(principal);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    public static JwtTokenProvider.AuthPrincipal getCurrentPrincipal() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtTokenProvider.AuthPrincipal p) return p;
        return null;
    }

    public static List<String> getCurrentRoles() {
        Authentication auth = getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .collect(Collectors.toList());
    }

    public static boolean hasRole(String role) {
        return getCurrentRoles().stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }
}
