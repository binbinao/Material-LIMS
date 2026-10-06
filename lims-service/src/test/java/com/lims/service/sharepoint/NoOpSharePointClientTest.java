package com.lims.service.sharepoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoOpSharePointClientTest {
    private final NoOpSharePointClient c = new NoOpSharePointClient();

    @Test
    void isEnabled_false() { assertThat(c.isEnabled()).isFalse(); }

    @Test
    void uploadDocx_throws() {
        assertThatThrownBy(() -> c.uploadDocx("x", new SharePointPath("", "x.docx")))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void downloadDocx_throws() {
        assertThatThrownBy(() -> c.downloadDocx("id"))
            .isInstanceOf(SharePointException.class);
    }

    @Test
    void composeEditUrl_throws() {
        assertThatThrownBy(() -> c.composeEditUrl("id"))
            .isInstanceOf(SharePointException.class);
    }
}