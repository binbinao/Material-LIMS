package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #58 (P5): {@code ReportService.createReport} must
 * validate the parent request's status. Today a user can generate a report
 * for a request in DRAFT/SUBMITTED/ASSIGNED/SAMPLING — none of which have
 * results worth reporting.
 *
 * The fix: at the top of {@code createReport}, load the request via
 * {@code requestMapper} and reject when status is not REPORTING,
 * APPROVING, or COMPLETED. Throw {@code REQUEST_STATUS_INVALID}.
 *
 * Asserted at source level — a full integration test would need a Postgres
 * fixture with rows in request + report tables.
 */
class ReportServiceCreateReportStatusTest {

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
    void createReportValidatesParentRequestStatus() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public Report createReport");
        assertTrue(idx > 0, "createReport method not found in ReportService");
        int bodyEnd = Math.min(src.length(), idx + 1500);
        String body = src.substring(idx, bodyEnd);
        boolean usesRequestMapper = body.contains("requestMapper")
                || body.contains("RequestMapper");
        boolean hasStatusCheck = body.contains("REQUEST_STATUS_INVALID")
                || body.contains("Request not in a reportable state");
        boolean mentionsReportableStatuses = body.contains("REPORTING")
                && body.contains("APPROVING")
                && body.contains("COMPLETED");
        assertTrue(usesRequestMapper && hasStatusCheck && mentionsReportableStatuses,
                "ReportService.createReport must load the parent request, " +
                        "check its status against REPORTING/APPROVING/COMPLETED, " +
                        "and throw REQUEST_STATUS_INVALID for other states. " +
                        "Today it accepts any request id without validating status.");
    }
}
