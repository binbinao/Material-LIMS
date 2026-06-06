package com.lims.web.security;

import com.lims.common.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Dev profile 专用：未登录时自动注入一个虚拟 ADMIN 用户，方便本地调试。
 * 不会在 prod 启用。
 */
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
