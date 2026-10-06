package com.lims.service.sync;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lims.service.sharepoint.SharePointProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class MicrosoftGraphClientDriveTest {

    private WireMockServer wm;
    private MicrosoftGraphClient client;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "contoso.sharepoint.com", "/sites/lims",
            "Documents", "Reports", "", "?action=edit"
        );
        RestTemplate rt = new RestTemplate();
        client = new MicrosoftGraphClient(rt);
        ReflectionTestUtils.setField(client, "graphBaseUrl", "http://localhost:" + wm.port() + "/v1.0");
        ReflectionTestUtils.setField(client, "tenantId", "tenant");
        ReflectionTestUtils.setField(client, "clientId", "client");
        ReflectionTestUtils.setField(client, "clientSecret", "secret");
        ReflectionTestUtils.setField(client, "configuredDriveId", "");
        ReflectionTestUtils.setField(client, "sharePointHostname", "contoso.sharepoint.com");
        ReflectionTestUtils.setField(client, "sharePointSitePath", "/sites/lims");
        ReflectionTestUtils.setField(client, "sharePointLibrary", "Documents");
        // Pre-seed token to bypass real MSAL call.
        ReflectionTestUtils.setField(client, "cachedToken", "fake-token");
        ReflectionTestUtils.setField(client, "tokenExpiresAtEpochMs", System.currentTimeMillis() + 3600_000L);
    }

    @AfterEach
    void tearDown() { wm.stop(); }

    @Test
    void putDriveItemContent_returns201Json() {
        wm.stubFor(put(urlMatching("/v1\\.0/drives/drv-1/root:.*REQ.*\\.docx:/content"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-123\",\"webUrl\":\"https://contoso.sharepoint.com/x\"}")));

        Map<String, Object> r = client.putDriveItemContent(
            "drv-1", "Reports/2026/01/", "REQ_V1.0.docx",
            "hello".getBytes(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertThat(r.get("id")).isEqualTo("item-123");
        assertThat(r.get("webUrl")).isEqualTo("https://contoso.sharepoint.com/x");
    }
}