package com.lims.web.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #50 (P3): {@code ReportController} must allow ADMIN
 * on endpoints where the documented role matrix says ADMIN inherits MANAGER.
 * Today three endpoints use {@code hasRole('MANAGER')} or a hard-coded
 * role list that omits {@code ADMIN}, so an ADMIN user gets 403 even
 * though they should be allowed.
 *
 * The fix: replace {@code hasRole('MANAGER')} with
 * {@code hasAnyRole('MANAGER','ADMIN')} on {@code approve} and
 * {@code reject}, and add {@code 'ADMIN'} to the {@code create}
 * endpoint's {@code hasAnyRole} list.
 *
 * Asserted at source level — a Spring Security integration test would
 * need a full {@code @WebMvcTest} slice and the project's other beans.
 */
class ReportControllerPreAuthorizeAdminTest {

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-web/src/main/java/com/lims/web/controller/ReportController.java");
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException("ReportController.java not found");
    }

    @Test
    void approveEndpointAllowsAdmin() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public R<Void> approve");
        assertTrue(idx > 0, "approve method not found in ReportController");
        int start = Math.max(0, idx - 250);
        String window = src.substring(start, idx);
        int preIdx = window.lastIndexOf("@PreAuthorize(");
        assertTrue(preIdx >= 0, "no @PreAuthorize before approve()");
        String pre = window.substring(preIdx, Math.min(window.length(), preIdx + 200));
        assertTrue(pre.contains("ADMIN"),
                "ReportController.approve must include ADMIN in its " +
                        "@PreAuthorize role list. Today it uses hasRole('MANAGER') " +
                        "which does NOT match the 'ADMIN' role, so ADMIN users get 403. " +
                        "Found: " + pre.trim());
    }

    @Test
    void rejectEndpointAllowsAdmin() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public R<Void> reject(");
        assertTrue(idx > 0, "reject method not found in ReportController");
        int start = Math.max(0, idx - 250);
        String window = src.substring(start, idx);
        int preIdx = window.lastIndexOf("@PreAuthorize(");
        assertTrue(preIdx >= 0, "no @PreAuthorize before reject()");
        String pre = window.substring(preIdx, Math.min(window.length(), preIdx + 200));
        assertTrue(pre.contains("ADMIN"),
                "ReportController.reject must include ADMIN in its @PreAuthorize. " +
                        "Found: " + pre.trim());
    }

    @Test
    void createEndpointAllowsAdmin() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public R<Report> create(");
        assertTrue(idx > 0, "create method not found in ReportController");
        int start = Math.max(0, idx - 250);
        String window = src.substring(start, idx);
        int preIdx = window.lastIndexOf("@PreAuthorize(");
        assertTrue(preIdx >= 0, "no @PreAuthorize before create()");
        String pre = window.substring(preIdx, Math.min(window.length(), preIdx + 200));
        assertTrue(pre.contains("ADMIN"),
                "ReportController.create must include ADMIN in its @PreAuthorize " +
                        "hasAnyRole list (today it only allows ENGINEER, MANAGER). " +
                        "Found: " + pre.trim());
    }
}
