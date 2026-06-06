package com.lims.service.sync;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Microsoft Graph 客户端（msal4j 取 Token + RestTemplate 调用 Graph REST API）。
 * 仅在 azure.ad.enabled=true 时启用，避免 dev 环境强制要求真实凭证。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "azure.ad.enabled", havingValue = "true")
public class MicrosoftGraphClient {

    private static final String GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";

    @Value("${azure.ad.tenant-id}")
    private String tenantId;

    @Value("${azure.ad.client-id}")
    private String clientId;

    @Value("${azure.ad.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private volatile String cachedToken;
    private volatile long tokenExpiresAtEpochMs;

    public MicrosoftGraphClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取 Graph access token（Client Credentials Flow）。简单缓存到过期前 60s。
     */
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAtEpochMs - 60_000) {
            return cachedToken;
        }
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                            clientId, ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority("https://login.microsoftonline.com/" + tenantId + "/")
                    .build();
            ClientCredentialParameters params = ClientCredentialParameters
                    .builder(Collections.singleton(GRAPH_DEFAULT_SCOPE))
                    .build();
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(params);
            IAuthenticationResult result = future.get();
            cachedToken = result.accessToken();
            tokenExpiresAtEpochMs = result.expiresOnDate().getTime();
            return cachedToken;
        } catch (Exception e) {
            log.error("Failed to acquire Graph access token", e);
            throw new RuntimeException("Failed to acquire Graph access token: " + e.getMessage(), e);
        }
    }

    /**
     * 列出所有启用用户（自动处理 @odata.nextLink 分页）。
     * @return Graph user 原始 Map，常用 key：id, displayName, mail, userPrincipalName, jobTitle, department
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listUsers() {
        String url = GRAPH_BASE + "/users?$select=id,displayName,mail,userPrincipalName,jobTitle,department&$top=999";
        List<Map<String, Object>> all = new ArrayList<>();
        while (url != null) {
            Map<String, Object> body = doGet(url);
            Object value = body.get("value");
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        all.add((Map<String, Object>) item);
                    }
                }
            }
            url = (String) body.get("@odata.nextLink");
        }
        return all;
    }

    /**
     * 列出所有组（用作部门）。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listGroups() {
        String url = GRAPH_BASE + "/groups?$select=id,displayName,description&$top=999";
        List<Map<String, Object>> all = new ArrayList<>();
        while (url != null) {
            Map<String, Object> body = doGet(url);
            Object value = body.get("value");
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        all.add((Map<String, Object>) item);
                    }
                }
            }
            url = (String) body.get("@odata.nextLink");
        }
        return all;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> doGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        if (resp.getBody() == null) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) resp.getBody();
    }
}
