package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #52 (P4): {@code ReportService.approveReport} must
 * reject self-approval. Today it only checks status; an author with a
 * MANAGER role (or the dev user with all roles) can approve their own
 * report — bypassing the four-eyes principle for compliance-sensitive
 * lab reports.
 *
 * The fix: in the approveReport method body, after the status check,
 * add a guard:
 * <pre>
 *   if (report.getAuthorId().equals(managerId)) {
 *     throw new BusinessException(ErrorCode.ACCESS_DENIED,
 *         "Approver must not be the report author");
 *   }
 * </pre>
 *
 * Asserted at source level — a full integration test would need an
 * H2/Postgres fixture with a Report row inserted.
 */
class ReportServiceSelfApprovalTest {

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-service/src/main/java/com/lims/service/ReportService.java");
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException("ReportService.java not found");
    }

    @Test
    void approveReportRejectsSelfApproval() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public void approveReport");
        assertTrue(idx > 0, "approveReport method not found in ReportService");
        int bodyEnd = Math.min(src.length(), idx + 1500);
        String body = src.substring(idx, bodyEnd);
        boolean hasAuthorCheck = body.contains("getAuthorId()")
                && body.contains("equals(managerId)");
        boolean hasAccessDenied = body.contains("ACCESS_DENIED")
                || body.contains("Approver must not be the report author");
        assertTrue(hasAuthorCheck && hasAccessDenied,
                "ReportService.approveReport must throw BusinessException " +
                        "(ACCESS_DENIED) when report.getAuthorId() equals managerId. " +
                        "Without this guard, an author with MANAGER/ADMIN role can " +
                        "self-approve their own report.");
    }
}
