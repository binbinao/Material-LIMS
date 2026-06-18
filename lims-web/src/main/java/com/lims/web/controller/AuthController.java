package com.lims.web.controller;

import com.lims.common.result.R;
import com.lims.common.security.JwtTokenProvider;
import com.lims.common.security.SecurityUtils;
import com.lims.model.entity.SysUser;
import com.lims.service.AuthService;
import com.lims.web.security.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "SSO认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /** 是否在 token cookie 上启用 secure 标记，prod 应为 true（必须 https） */
    @Value("${security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @GetMapping("/azure-ad-login")
    @Operation(summary = "Get Azure AD login redirect URL")
    public R<Map<String, String>> azureAdLogin(HttpSession session) {
        return R.ok(Map.of("authorizationUrl", authService.getAuthorizationUrl(session)));
    }

    @PostMapping("/callback")
    @Operation(summary = "Azure AD OAuth callback endpoint (response_mode=form_post)")
    public R<Map<String, Object>> callback(@RequestParam String code,
                                           @RequestParam String state,
                                           HttpSession session,
                                           HttpServletResponse response) {
        Map<String, Object> result = authService.handleCallback(code, state, session);
        // Set httpOnly cookie so subsequent requests carry the token
        Object tokenObj = result.get("token");
        if (tokenObj instanceof String token) {
            Cookie cookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(cookieSecure);
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtTokenProvider.getTtlHours() * 3600));
            response.addCookie(cookie);
        }
        return R.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout - clear LIMS_TOKEN cookie")
    public R<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtAuthenticationFilter.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return R.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public R<SysUser> me() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return R.fail(3001, "Not authenticated");
        }
        SysUser user = authService.getCurrentUser(userId);
        return R.ok(user);
    }

    @PutMapping("/me/locale")
    @Operation(summary = "Switch user interface language")
    public R<Void> updateLocale(@RequestBody Map<String, String> body) {
        // TODO: persist locale preference on sys_user when locale column is added.
        return R.ok();
    }
}
