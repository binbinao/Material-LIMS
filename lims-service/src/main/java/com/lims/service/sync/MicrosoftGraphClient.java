package com.lims.service.sync;

import com.lims.service.sharepoint.SharePointException;
import com.lims.service.sharepoint.SharePointProperties;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Value("${azure.ad.graph-base-url:https://graph.microsoft.com/v1.0}")
    private String graphBaseUrl;

    @Value("${azure.ad.tenant-id}")
    private String tenantId;

    @Value("${azure.ad.client-id}")
    private String clientId;

    @Value("${azure.ad.client-secret}")
    private String clientSecret;

    @Value("${sharepoint.drive-id:}")
    private String configuredDriveId;

    @Value("${sharepoint.hostname:}")
    private String sharePointHostname;

    @Value("${sharepoint.site-path:/sites/lims}")
    private String sharePointSitePath;

    @Value("${sharepoint.library:Documents}")
    private String sharePointLibrary;

    private final RestTemplate restTemplate;
    private volatile String cachedToken;
    private volatile long tokenExpiresAtEpochMs;
    private volatile String driveIdCache;

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
        String url = graphBaseUrl + "/users?$select=id,displayName,mail,userPrincipalName,jobTitle,department&$top=999";
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
        String url = graphBaseUrl + "/groups?$select=id,displayName,description&$top=999";
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
     * Resolve the drive id for the configured SharePoint site + library.
     * Uses configuredDriveId if non-blank; otherwise resolves via /sites/.../drives.
     */
    public synchronized String resolveDriveId() {
        if (configuredDriveId != null && !configuredDriveId.isBlank()) {
            return configuredDriveId;
        }
        if (driveIdCache != null) {
            return driveIdCache;
        }
        String siteUrl = graphBaseUrl + "/sites/" + sharePointHostname + ":" + sharePointSitePath + "?$select=id";
        Map<String, Object> site = doGet(siteUrl);
        String siteId = (String) site.get("id");
        if (siteId == null) {
            throw new SharePointException("Graph site resolution failed: no id in response");
        }
        String drivesUrl = graphBaseUrl + "/sites/" + siteId + "/drives?$filter=" + encodeODataFilter("name eq '" + sharePointLibrary + "'");
        Map<String, Object> drivesResp = doGet(drivesUrl);
        Object value = drivesResp.get("value");
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new SharePointException("Graph drives: no drive matching library '" + sharePointLibrary + "'");
        }
        Object first = list.get(0);
        if (!(first instanceof Map)) {
            throw new SharePointException("Graph drives: malformed response");
        }
        Object id = ((Map<?, ?>) first).get("id");
        if (id == null) {
            throw new SharePointException("Graph drives: first item has no id");
        }
        driveIdCache = id.toString();
        return driveIdCache;
    }

    /**
     * PUT a docx (or other binary) into drive root at the given path. Returns driveItem JSON.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> putDriveItemContent(String driveId, String parentPath, String filename,
                                                   byte[] body, String contentType) {
        String url = graphBaseUrl + "/drives/" + driveId + "/root:/" + encodeGraphPath(parentPath) + encodeGraphPath(filename) + ":/content";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.parseMediaType(contentType));
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
        if (resp.getBody() == null) {
            throw new SharePointException("Graph PUT returned empty body");
        }
        return (Map<String, Object>) resp.getBody();
    }

    /**
     * GET a driveItem by id. `select` should be a comma-separated field list, e.g. "webUrl".
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDriveItem(String driveId, String itemId, String select) {
        String url = graphBaseUrl + "/drives/" + driveId + "/items/" + itemId;
        if (select != null && !select.isBlank()) {
            url += "?$select=" + select;
        }
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(bearerHeaders()), Map.class);
        if (resp.getBody() == null) {
            throw new SharePointException("Graph GET item returned empty body");
        }
        return (Map<String, Object>) resp.getBody();
    }

    /**
     * GET a driveItem by parent path + filename. Used for 409-conflict fallback.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDriveItemByPath(String driveId, String parentPath, String filename) {
        String url = graphBaseUrl + "/drives/" + driveId + "/root:/" + encodeGraphPath(parentPath) + encodeGraphPath(filename);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(bearerHeaders()), Map.class);
        if (resp.getBody() == null) {
            throw new SharePointException("Graph GET item-by-path returned empty body");
        }
        return (Map<String, Object>) resp.getBody();
    }

    /**
     * Download the content of a driveItem.
     */
    public byte[] getDriveItemContent(String driveId, String itemId) {
        String url = graphBaseUrl + "/drives/" + driveId + "/items/" + itemId + "/content";
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()), byte[].class);
            return resp.getBody();
        } catch (Exception e) {
            throw new SharePointException("Graph download failed: " + e.getMessage(), e);
        }
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

    private HttpHeaders bearerHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getAccessToken());
        return h;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Encode a single path segment for a Graph drive path. Encodes characters that would otherwise
     * confuse the URL parser, but preserves '/' so segment boundaries stay readable.
     */
    private static String encodeGraphPath(String segment) {
        if (segment == null || segment.isEmpty()) return "";
        return URLEncoder.encode(segment, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%2F", "/");
    }

    private static String encodeODataFilter(String filter) {
        return URLEncoder.encode(filter, StandardCharsets.UTF_8).replace("+", "%20");
    }
}