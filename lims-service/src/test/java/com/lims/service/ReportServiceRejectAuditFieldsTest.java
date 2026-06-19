package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #64 (P7): {@code ReportService.rejectReport} must
 * record who rejected and when — matching the audit trail {@code approveReport}
 * already writes via {@code approvedBy} / {@code approvedAt}.
 *
 * Asserted at source level across 3 files:
 *   - ReportService.java: rejectReport body sets rejectedBy + rejectedAt
 *   - Report.java:        entity has rejectedBy + rejectedAt fields
 *   - V3 migration:       adds rejected_by + rejected_at columns
 */
class ReportServiceRejectAuditFieldsTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void rejectReportSetsRejectedByAndRejectedAt() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/ReportService.java");
        int idx = src.indexOf("public void rejectReport");
        assertTrue(idx > 0, "rejectReport method not found");
        int bodyEnd = Math.min(src.length(), idx + 1500);
        String body = src.substring(idx, bodyEnd);
        assertTrue(body.contains("setRejectedBy"),
                "rejectReport must call setRejectedBy(managerId)");
        assertTrue(body.contains("setRejectedAt"),
                "rejectReport must call setRejectedAt(now)");
        int sigIdx = src.indexOf("public void rejectReport");
        int sigEnd = Math.min(src.length(), sigIdx + 200);
        String sig = src.substring(sigIdx, sigEnd);
        assertTrue(sig.contains("String managerId"),
                "rejectReport signature must include managerId parameter");
    }

    @Test
    void reportEntityHasRejectedByAndRejectedAtFields() throws Exception {
        String src = readSource(
                "lims-model/src/main/java/com/lims/model/entity/Report.java");
        assertTrue(src.contains("rejectedBy"),
                "Report entity must declare rejectedBy field");
        assertTrue(src.contains("rejectedAt"),
                "Report entity must declare rejectedAt field");
    }

    @Test
    void flywayV3MigrationAddsRejectedColumns() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path[] found = new Path[1];
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path migrationDir = p.resolve(
                    "lims-web/src/main/resources/db/migration");
            if (Files.isDirectory(migrationDir)) {
                Files.list(migrationDir)
                        .filter(f -> f.getFileName().toString().startsWith("V3"))
                        .findFirst()
                        .ifPresent(f -> found[0] = f);
                break;
            }
        }
        assertTrue(found[0] != null,
                "Flyway V3 migration adding rejected_by + rejected_at columns must exist");
        String mig = Files.readString(found[0]);
        assertTrue(mig.contains("rejected_by"),
                "V3 migration must add rejected_by column");
        assertTrue(mig.contains("rejected_at"),
                "V3 migration must add rejected_at column");
        assertTrue(mig.contains("report"),
                "V3 migration must target the report table");
    }
}
