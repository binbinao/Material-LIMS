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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("permitAll()")
    @Operation(summary = "(disabled) Azure AD SSO login — returns 410")
    public R<Map<String, String>> azureAdLogin(HttpSession session) {
        // SSO has been turned off per product decision. The endpoint
        // remains in the code so a future product owner can re-enable
        // by setting `azure.ad.enabled=true` and wiring the client
        // secret / tenant id. For now we reject with 410 Gone so any
        // stale bookmark / cached URL is loud rather than silent.
        throw new com.lims.common.exception.BusinessException(
                com.lims.common.exception.ErrorCode.M365_INTEGRATION_ERROR,
                "SSO login is disabled. Use POST /api/v1/auth/login with loginId and password.");
    }

    @GetMapping("/azure-ad/url")
    @PreAuthorize("permitAll()")
    @Operation(summary = "(disabled) Azure AD SSO login — historical frontend URL, returns 410")
    public R<Map<String, String>> azureAdUrlAlias() {
        // The frontend used to call /auth/azure-ad/url; the old backend
        // route was /auth/azure-ad-login. Both are now disabled.
        throw new com.lims.common.exception.BusinessException(
                com.lims.common.exception.ErrorCode.M365_INTEGRATION_ERROR,
                "SSO login is disabled. Use POST /api/v1/auth/login with loginId and password.");
    }

    @PostMapping("/callback")
    @PreAuthorize("permitAll()")
    @Operation(summary = "(disabled) Azure AD OAuth callback — returns 410")
    public R<Map<String, Object>> callback(@RequestParam String code,
                                           @RequestParam String state,
                                           HttpSession session,
                                           HttpServletResponse response) {
        throw new com.lims.common.exception.BusinessException(
                com.lims.common.exception.ErrorCode.M365_INTEGRATION_ERROR,
                "SSO callback is disabled. Use POST /api/v1/auth/login with loginId and password.");
    }

    @PostMapping("/azure-ad/callback")
    @PreAuthorize("permitAll()")
    @Operation(summary = "(disabled) Azure AD OAuth callback — historical frontend URL, returns 410")
    public R<Map<String, Object>> azureAdCallbackAlias() {
        throw new com.lims.common.exception.BusinessException(
                com.lims.common.exception.ErrorCode.M365_INTEGRATION_ERROR,
                "SSO callback is disabled. Use POST /api/v1/auth/login with loginId and password.");
    }

    /**
     * Manual password login. The default password for every seeded
     * account is "password" (see V15). Issues the same LIMS JWT as
     * the (now-disabled) SSO callback and sets the auth cookie.
     */
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Login with loginId + password; returns LIMS JWT and sets auth cookie")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                        HttpServletResponse response) {
        String loginId = body.get("loginId");
        String password = body.get("password");
        Map<String, Object> result = authService.login(loginId, password);
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

    /**
     * Self-service password change. Caller is identified from the
     * SecurityContext (the auth cookie). Requires the current password.
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change the current user's password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(
                SecurityUtils.getCurrentUserId(),
                body.get("oldPassword"),
                body.get("newPassword"));
        return R.ok();
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Switch user interface language")
    public R<Void> updateLocale(@RequestBody Map<String, String> body) {
        // TODO: persist locale preference on sys_user when locale column is added.
        return R.ok();
    }
}
