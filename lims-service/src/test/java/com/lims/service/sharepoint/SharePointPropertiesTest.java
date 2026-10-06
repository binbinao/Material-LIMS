package com.lims.service.sharepoint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SharePointPropertiesTest.Config.class)
@TestPropertySource(properties = {
    "sharepoint.enabled=true",
    "sharepoint.strategy=flat",
    "sharepoint.hostname=contoso.sharepoint.com",
    "sharepoint.site-path=/sites/lims",
    "sharepoint.library=Documents",
    "sharepoint.folder-root=Reports",
    "sharepoint.drive-id=drv-abc",
    "sharepoint.edit-url-suffix=?action=edit"
})
class SharePointPropertiesTest {

    @Autowired
    SharePointProperties props;

    @Test
    void bindsAllFields() {
        assertThat(props.enabled()).isTrue();
        assertThat(props.strategy()).isEqualTo("flat");
        assertThat(props.hostname()).isEqualTo("contoso.sharepoint.com");
        assertThat(props.sitePath()).isEqualTo("/sites/lims");
        assertThat(props.library()).isEqualTo("Documents");
        assertThat(props.folderRoot()).isEqualTo("Reports");
        assertThat(props.driveId()).isEqualTo("drv-abc");
        assertThat(props.editUrlSuffix()).isEqualTo("?action=edit");
    }

    @EnableConfigurationProperties(SharePointProperties.class)
    static class Config {}
}