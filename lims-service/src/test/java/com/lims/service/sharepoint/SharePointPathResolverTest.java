package com.lims.service.sharepoint;

import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SharePointPathResolverTest {

    private final SharePointProperties props = new SharePointProperties(
        true, "folder_layout", "", "/sites/lims", "Documents", "Reports", "", "?action=edit"
    );
    private final SharePointPathResolver resolver = new SharePointPathResolver(props);

    private Request req(int year, int month) {
        Request r = new Request();
        r.setRequestNo("REQ-2026-0001");
        r.setCreatedAt(LocalDateTime.of(year, month, 15, 0, 0));
        return r;
    }

    private Report report(String version) {
        Report rep = new Report();
        rep.setVersionNumber(version);
        return rep;
    }

    @Test
    void folderLayout_january() {
        var p = resolver.resolve(req(2026, 1), report("V1.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2026/01/");
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V1.0.docx");
    }

    @Test
    void folderLayout_december() {
        var p = resolver.resolve(req(2026, 12), report("V2.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2026/12/");
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V2.0.docx");
    }

    @Test
    void folderLayout_leapFebruary() {
        var p = resolver.resolve(req(2024, 2), report("V1.0"));
        assertThat(p.driveRelativePath()).isEqualTo("Reports/2024/02/");
    }

    @Test
    void flatStrategy() {
        SharePointProperties flat = new SharePointProperties(
            true, "flat", "", "/sites/lims", "Documents", "Reports", "", "?action=edit"
        );
        SharePointPathResolver r = new SharePointPathResolver(flat);
        var p = r.resolve(req(2026, 5), report("V3.0"));
        assertThat(p.driveRelativePath()).isEmpty();
        assertThat(p.filename()).isEqualTo("REQ-2026-0001_V3.0.docx");
    }
}