package com.lims.service.sharepoint;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lims.service.sync.MicrosoftGraphClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphSharePointClientTest {

    private WireMockServer wm;
    private GraphSharePointClient client;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        SharePointProperties props = new SharePointProperties(
            true, "folder_layout", "contoso.sharepoint.com", "/sites/lims",
            "Documents", "Reports", "drv-1", "?action=edit"
        );
        MicrosoftGraphClient mgc = new MicrosoftGraphClient(new org.springframework.web.client.RestTemplate());
        ReflectionTestUtils.setField(mgc, "graphBaseUrl", "http://localhost:" + wm.port() + "/v1.0");
        ReflectionTestUtils.setField(mgc, "tenantId", "t");
        ReflectionTestUtils.setField(mgc, "clientId", "c");
        ReflectionTestUtils.setField(mgc, "clientSecret", "s");
        ReflectionTestUtils.setField(mgc, "configuredDriveId", "drv-1");
        ReflectionTestUtils.setField(mgc, "sharePointHostname", "contoso.sharepoint.com");
        ReflectionTestUtils.setField(mgc, "sharePointSitePath", "/sites/lims");
        ReflectionTestUtils.setField(mgc, "sharePointLibrary", "Documents");
        ReflectionTestUtils.setField(mgc, "cachedToken", "fake-token");
        ReflectionTestUtils.setField(mgc, "tokenExpiresAtEpochMs", System.currentTimeMillis() + 3600_000L);
        client = new GraphSharePointClient(mgc, props);
    }

    @AfterEach
    void tearDown() { wm.stop(); }

    @Test
    void uploadDocx_happyPath_returnsFileIdAndEditUrl() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*REQ.*\\.docx:/content"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-1\",\"webUrl\":\"https://contoso.sharepoint.com/x\"}")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake-docx");

        var r = client.uploadDocx(docx.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        assertThat(r.fileId()).isEqualTo("item-1");
        assertThat(r.editUrl()).isEqualTo("https://contoso.sharepoint.com/x?action=edit");
    }

    @Test
    void uploadDocx_409Conflicts_fallsBackToGetExisting() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(409)));
        wm.stubFor(get(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"item-existing\",\"webUrl\":\"https://contoso.sharepoint.com/existing\"}")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake");

        var r = client.uploadDocx(docx.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        assertThat(r.fileId()).isEqualTo("item-existing");
    }

    @Test
    void uploadDocx_5xx_throwsSharePointException() throws Exception {
        wm.stubFor(put(urlMatching("/v1.0/drives/drv-1/root:.*"))
            .willReturn(aResponse().withStatus(500).withBody("server error")));

        Path docx = Files.createTempFile("test", ".docx");
        Files.writeString(docx, "fake");

        assertThatThrownBy(() -> client.uploadDocx(docx.toString(),
                new SharePointPath("Reports/", "REQ_V1.0.docx")))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void composeEditUrl_appendsSuffix() {
        wm.stubFor(get(urlEqualTo("/v1.0/drives/drv-1/items/item-9?$select=webUrl"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"webUrl\":\"https://contoso.sharepoint.com/y\"}")));

        assertThat(client.composeEditUrl("item-9")).isEqualTo("https://contoso.sharepoint.com/y?action=edit");
    }
}