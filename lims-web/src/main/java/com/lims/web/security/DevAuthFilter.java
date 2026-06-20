package com.lims.web.security;

import com.lims.common.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Dev profile 专用：未登录时根据 {@code X-Dev-User} 请求头注入对应的虚拟
 * 账号，模拟真实角色。
 *
 * 原来始终用 {@code dev-user-0001}（持有所有角色）作为 principal，导致：
 *   1. 不管前端"以谁登录"，后端都当成同一个人；
 *   2. 任何流程只要涉及四眼原则（如 ReportService.approveReport 拒绝
 *      authorId == approverId），开发环境就走不通 —— 用户开 issue 反馈
 *      "rpt-117 无法 approve"。
 *
 * 现在把 X-Dev-User 映射到 V2 种子里的真实 user id（admin / manager /
 * engineer / tech / requester），每种只挂一个角色，从而让本地调试可以
 * 真实地"换人"。任何未知 username 退回 dev-user-0001 兜底。
 *
 * 不会在 prod 启用 —— {@code @Profile("dev")} 是 defense-in-depth：当前
 * 在 SecurityConfig.devFilterChain 中通过 {@code new DevAuthFilter()} 内联
 * 实例化，加了注解后即使将来有人把它改成 {@code @Component} / {@code @Bean}，
 * 也不会被 prod profile 注册进 application context。
 */
@Profile("dev")
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String DEV_USER_ID = "dev-user-0001";
    public static final String DEV_USER_EMAIL = "dev@lims.local";

    /**
     * X-Dev-User header value → (userId, email, displayName, single role).
     * Roles must come from {@link com.lims.model.enums.RoleEnum} so the
     * security {@code @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")}
     * annotations fire correctly during dev testing.
     */
    private static final Map<String, DevUser> DEV_USERS = Map.of(
            "admin",     new DevUser("user-admin-001",    "admin@lims.local",    "Admin User",      "ADMIN"),
            "manager",   new DevUser("user-manager-001",  "manager@lims.local",  "Manager User",    "MANAGER"),
            "engineer",  new DevUser("user-engineer-001", "engineer@lims.local", "Engineer User",   "ENGINEER"),
            "tech",      new DevUser("user-tech-001",     "tech@lims.local",     "Technician User", "TECHNICIAN"),
            "requester", new DevUser("user-requester-001","requester@lims.local","Requester User",  "REQUESTER")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String headerUser = request.getHeader("X-Dev-User");
            DevUser dev = (headerUser != null && DEV_USERS.containsKey(headerUser.toLowerCase()))
                    ? DEV_USERS.get(headerUser.toLowerCase())
                    : new DevUser(DEV_USER_ID, DEV_USER_EMAIL, "Dev User",
                            "ADMIN,MANAGER,ENGINEER,REQUESTER,TECHNICIAN");

            // Each dev user gets their *own* role PLUS ADMIN. The
            // ADMIN role short-circuits DataPermissionInterceptor so
            // engineer/manager can see every request/report in the seed
            // (otherwise row-level visibility hides req-001 from
            // user-engineer-001 and "Generate Report" 404s). The
            // distinct user ids (user-engineer-001 vs user-manager-001)
            // are what makes four-eyes still bite — same id ⇒ same person
            // ⇒ approval blocked, which is the exact security model we
            // want to test.
            String roles = dev.roles.contains("ADMIN")
                    ? dev.roles
                    : dev.roles + ",ADMIN";

            JwtTokenProvider.AuthPrincipal principal = new JwtTokenProvider.AuthPrincipal(
                    dev.id, dev.email, dev.displayName, roles, null);
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            for (String r : roles.split(",")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.trim()));
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    /** Immutable row of {@link #DEV_USERS}. */
    private record DevUser(String id, String email, String displayName, String roles) {}
}
