package com.lims.service.sharepoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockSharePointClientTest {

    @TempDir
    Path tmp;

    private MockSharePointClient client;

    @BeforeEach
    void setUp() {
        client = new MockSharePointClient(tmp);
    }

    @Test
    void isEnabled_true() {
        assertThat(client.isEnabled()).isTrue();
    }

    @Test
    void uploadDocx_returnsDeterministicIdAndMockUrl() throws Exception {
        Path src = tmp.resolve("src.docx");
        Files.writeString(src, "hello-world");

        var r = client.uploadDocx(src.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        assertThat(r.fileId()).startsWith("mock-");
        assertThat(r.editUrl()).startsWith("https://example.invalid/mock-sharepoint/");
        assertThat(r.editUrl()).endsWith("?action=edit");
        assertThat(r.editUrl()).contains(r.fileId());
    }

    @Test
    void uploadDocx_copiesSourceFileToLocalTmp() throws Exception {
        Path src = tmp.resolve("src.docx");
        Files.writeString(src, "hello");

        var r = client.uploadDocx(src.toString(),
            new SharePointPath("Reports/2026/01/", "REQ-2026-0001_V1.0.docx"));

        Path copied = tmp.resolve(r.fileId() + ".docx");
        assertThat(copied).exists();
        assertThat(Files.readString(copied)).isEqualTo("hello");
    }

    @Test
    void downloadDocx_readsBackCopiedBytes() throws Exception {
        Path src = tmp.resolve("y.docx");
        Files.writeString(src, "downloaded-body");

        var r = client.uploadDocx(src.toString(),
            new SharePointPath("", "REQ_V1.0.docx"));

        byte[] body = client.downloadDocx(r.fileId());
        assertThat(new String(body)).isEqualTo("downloaded-body");
    }

    @Test
    void downloadDocx_unknownId_throws() {
        assertThatThrownBy(() -> client.downloadDocx("mock-not-real"))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void composeEditUrl_returnsStableMockUrl() {
        assertThat(client.composeEditUrl("mock-123"))
            .isEqualTo("https://example.invalid/mock-sharepoint/mock-123?action=edit");
    }
}