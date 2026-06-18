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

/**
 * Dev profile 专用：未登录时自动注入一个虚拟 ADMIN 用户，方便本地调试。
 * 不会在 prod 启用。{@code @Profile("dev")} 是 defense-in-depth：当前在
 * SecurityConfig.devFilterChain 中通过 {@code new DevAuthFilter()} 内联实例化，
 * 加了注解后即使将来有人把它改成 {@code @Component} / {@code @Bean}，也不会被
 * prod profile 注册进 application context。
 */
@Profile("dev")
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String DEV_USER_ID = "dev-user-0001";
    public static final String DEV_USER_EMAIL = "dev@lims.local";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtTokenProvider.AuthPrincipal principal = new JwtTokenProvider.AuthPrincipal(
                    DEV_USER_ID, DEV_USER_EMAIL, "Dev User",
                    "ADMIN,MANAGER,ENGINEER,REQUESTER,TECHNICIAN", null);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_MANAGER"),
                            new SimpleGrantedAuthority("ROLE_ENGINEER"),
                            new SimpleGrantedAuthority("ROLE_REQUESTER"),
                            new SimpleGrantedAuthority("ROLE_TECHNICIAN")
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
