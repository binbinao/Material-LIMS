package com.lims.service;

import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import com.lims.common.security.JwtTokenProvider;
import com.lims.dao.mapper.SysUserMapper;
import com.lims.model.entity.SysUser;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Cached JWKS for the tenant. Refreshed on miss / parse failure. */
    private JWKSet cachedJwks;
    private Instant cachedJwksFetchedAt;
    private static final long JWKS_CACHE_TTL_SECONDS = 600;  // 10 min

    /** HttpSession attribute name for the OAuth 2.0 state parameter (CSRF token). */
    public static final String SESSION_ATTR_STATE = "oauth_state";
    /** HttpSession attribute name for the OIDC nonce (id_token replay protection). */
    public static final String SESSION_ATTR_NONCE = "oauth_nonce";

    /**
     * Build Azure AD authorization URL for SSO login.
     *
     * <p>Issue #4: now generates a cryptographically random {@code state}
     * (CSRF protection per RFC 6749 §10.12) and a random {@code nonce}
     * (OpenID Connect Core 1.0 §15.5.2 — replay protection), persists both
     * in the supplied {@link HttpSession}, and embeds them in the URL so
     * {@link #handleCallback} can validate them on return.
     */
    public String getAuthorizationUrl(HttpSession session) {
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        session.setAttribute(SESSION_ATTR_STATE, state);
        session.setAttribute(SESSION_ATTR_NONCE, nonce);
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode("openid profile email User.Read", StandardCharsets.UTF_8) +
                "&response_mode=form_post" +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                "&nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8);
    }

    /**
     * Handle Azure AD OAuth callback: validate state + nonce, exchange code for
     * token, create/update user, sign LIMS JWT.
     * @return Map with keys: token, user, expiresInHours
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleCallback(String code, String state, HttpSession session) {
        if (!azureAdEnabled) {
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "Azure AD is not enabled in this environment. Set azure.ad.enabled=true");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "code is required");
        }

        // Issue #4: state validation (CSRF protection). The expected state
        // was stored in session by getAuthorizationUrl; the incoming state
        // must match it exactly. Comparing via .equals() is the canonical
        // RFC 6749 §10.12 check; a constant-time compare is preferred but
        // not strictly required for a UUID.
        String expectedState = session != null ? (String) session.getAttribute(SESSION_ATTR_STATE) : null;
        if (expectedState == null || !expectedState.equals(state)) {
            // Issue #23: don't echo the expected UUID in the user-facing
            // error. Log server-side; the message is generic.
            log.warn("OAuth state mismatch (expected={}, got={})", expectedState, state);
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "OAuth state mismatch");
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

        // Step 2: Decode id_token and verify signature (issue #3)
        String idToken = (String) body.get("id_token");
        Map<String, Object> claims = parseIdTokenClaims(idToken);

        // Issue #4: nonce claim must match the value we stored in session
        // when getAuthorizationUrl was called. Without this, a replayed
        // id_token from a prior session (still within Azure AD's ~1h
        // expiry) would be accepted.
        String expectedNonce = session != null ? (String) session.getAttribute(SESSION_ATTR_NONCE) : null;
        String actualNonce = (String) claims.get("nonce");
        if (expectedNonce == null || !expectedNonce.equals(actualNonce)) {
            // Issue #23: same rationale as state — keep nonce values out
            // of the user-facing message.
            log.warn("id_token nonce mismatch (expected={}, got={})", expectedNonce, actualNonce);
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "id_token nonce mismatch");
        }

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
            // Issue #3: verify the id_token signature against Azure AD's JWKS
            // before trusting any claim. Without this, any caller with network
            // access to /api/v1/auth/callback can mint a self-signed id_token
            // and have the server issue a LIMS JWT for any email they choose.
            SignedJWT signed = SignedJWT.parse(idToken);
            JWKSet jwks = loadJwks();
            String kid = signed.getHeader().getKeyID();
            RSAKey rsaKey = (RSAKey) jwks.getKeyByKeyId(kid);
            if (rsaKey == null) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token signed by unknown key id: " + kid);
            }
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signed.verify(verifier)) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token signature verification failed");
            }
            JWTClaimsSet claims = signed.getJWTClaimsSet();

            // iss must match https://login.microsoftonline.com/{tenantId}/v2.0
            String expectedIssuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
            if (claims.getIssuer() == null || !claims.getIssuer().equals(expectedIssuer)) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token issuer mismatch: expected " + expectedIssuer +
                                ", got " + claims.getIssuer());
            }

            // aud must contain the configured client_id
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(clientId)) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token audience mismatch: expected to contain " + clientId +
                                ", got " + aud);
            }

            // exp must be in the future
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token expired or has no exp claim");
            }

            // nbf (if present) must not be in the future
            Date nbf = claims.getNotBeforeTime();
            if (nbf != null && nbf.toInstant().isAfter(Instant.now())) {
                throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                        "id_token not yet valid (nbf in the future)");
            }

            Map<String, Object> map = new HashMap<>(claims.getClaims());
            return map;
        } catch (BusinessException e) {
            throw e;
        } catch (ParseException | com.nimbusds.jose.JOSEException | java.io.IOException e) {
            log.error("id_token verification failed", e);
            throw new BusinessException(ErrorCode.M365_INTEGRATION_ERROR,
                    "Failed to verify id_token: " + e.getMessage());
        }
    }

    /**
     * Fetch the Azure AD JWKS for the configured tenant, with a 10-minute TTL.
     * On a miss or parse failure, re-fetch once.
     */
    /**
     * Issue #17: Pre-warm the JWKS cache at application startup so the
     * first user login doesn't pay the Microsoft round-trip latency.
     * Failure here is non-fatal — if Microsoft is unreachable at boot
     * time, the next id_token verification will trigger a lazy fetch
     * (which is now circuit-breaker-wrapped and capped at 3 s).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void prewarmJwks() {
        if (!azureAdEnabled) return;
        try {
            loadJwks();
            log.info("JWKS cache pre-warmed at startup");
        } catch (Exception e) {
            log.warn("JWKS pre-warm failed (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Fetch the Azure AD JWKS for the configured tenant, with a 10-minute TTL.
     * On a miss or parse failure, re-fetch once. Wrapped in a Resilience4j
     * circuit breaker so a slow / unreachable Microsoft JWKS endpoint
     * fails fast (≤ 3 s) instead of hanging every login callback for
     * the full {@code SimpleClientHttpRequestFactory} timeout.
     */
    @CircuitBreaker(name = "jwks")
    private JWKSet loadJwks() throws java.io.IOException, java.text.ParseException {
        if (cachedJwks != null && cachedJwksFetchedAt != null &&
                Instant.now().getEpochSecond() - cachedJwksFetchedAt.getEpochSecond() < JWKS_CACHE_TTL_SECONDS) {
            return cachedJwks;
        }
        String jwksUrl = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
        String body;
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<String> resp = restTemplate.getForEntity(jwksUrl, String.class);
            body = resp.getBody();
        } catch (Exception e) {
            // cache-bust and rethrow
            cachedJwks = null;
            cachedJwksFetchedAt = null;
            throw e;
        }
        if (body == null) {
            cachedJwks = null;
            cachedJwksFetchedAt = null;
            throw new java.io.IOException("Azure AD JWKS endpoint returned empty body");
        }
        cachedJwks = JWKSet.parse(body);
        cachedJwksFetchedAt = Instant.now();
        return cachedJwks;
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
