package com.lims.service;

import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.JwtTokenProvider;
import com.lims.dao.mapper.SysUserMapper;
import com.lims.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${azure.ad.tenant-id}")
    private String tenantId;

    @Value("${azure.ad.client-id}")
    private String clientId;

    @Value("${azure.ad.client-secret}")
    private String clientSecret;

    @Value("${azure.ad.redirect-uri}")
    private String redirectUri;

    @Value("${azure.ad.enabled:false}")
    private boolean azureAdEnabled;

    /**
     * Build Azure AD authorization URL for SSO login
     */
    public String getAuthorizationUrl() {
        return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize" +
                "?client_id=%s" +
                "&response_type=code" +
                "&redirect_uri=%s" +
                "&scope=%s" +
                "&response_mode=query",
                tenantId, clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode("openid profile email User.Read", StandardCharsets.UTF_8));
    }

    /**
     * Handle Azure AD OAuth callback: exchange code for token, create/update user, sign LIMS JWT.
     * @return Map with keys: token, user, expiresInHours
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleCallback(String code) {
        if (!azureAdEnabled) {
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "Azure AD is not enabled in this environment. Set azure.ad.enabled=true");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "code is required");
        }

        // Step 1: Exchange code for tokens
        String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri);
        form.add("scope", "openid profile email User.Read");

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    tokenUrl, org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(form, headers), Map.class);
        } catch (Exception e) {
            log.error("Azure AD token exchange failed", e);
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "Token exchange failed: " + e.getMessage());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        if (body == null || body.get("id_token") == null) {
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR, "id_token missing in token response");
        }

        // Step 2: Decode id_token (no signature verification here; Azure AD already returned via TLS)
        // Production hardening: verify signature against Microsoft JWKS (jwks_uri).
        String idToken = (String) body.get("id_token");
        Map<String, Object> claims = parseIdTokenClaims(idToken);

        String email = firstNonBlank(
                (String) claims.get("email"),
                (String) claims.get("preferred_username"),
                (String) claims.get("upn"));
        String displayName = firstNonBlank((String) claims.get("name"), email);
        String externalId = (String) claims.get("oid");
        if (externalId == null) externalId = (String) claims.get("sub");

        if (email == null) {
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR, "email claim missing in id_token");
        }

        // Step 3: Find or create local user
        SysUser user = findOrCreateUser(email, displayName, externalId, null);

        // Step 4: Sign LIMS JWT
        String token = jwtTokenProvider.generate(
                user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRoles(), user.getDeptId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("expiresInHours", jwtTokenProvider.getTtlHours());
        log.info("Azure AD SSO success: userId={}, email={}", user.getId(), email);
        return result;
    }

    private Map<String, Object> parseIdTokenClaims(String idToken) {
        try {
            cn.hutool.json.JSONObject json = JWTUtil.parseToken(idToken).getPayloads();
            Map<String, Object> map = new HashMap<>();
            for (String key : json.keySet()) {
                map.put(key, json.get(key));
            }
            return map;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "Failed to parse id_token: " + e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    /**
     * Find or create user from Azure AD info
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser findOrCreateUser(String email, String displayName, String externalId, String deptId) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));

        if (user != null) {
            user.setDisplayName(displayName);
            if (externalId != null) user.setExternalId(externalId);
            if (deptId != null) user.setDeptId(deptId);
            user.setLastLoginAt(LocalDateTime.now());
            sysUserMapper.updateById(user);
            return user;
        }

        // Create new user with default REQUESTER role
        user = new SysUser();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setExternalId(externalId);
        user.setDeptId(deptId);
        user.setRoles("REQUESTER");
        user.setIsActive(true);
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        log.info("Created new user from Azure AD: email={}", email);
        return user;
    }

    /**
     * Get current user info by id
     */
    public SysUser getCurrentUser(String userId) {
        if (userId == null) return null;
        return sysUserMapper.selectById(userId);
    }
}
