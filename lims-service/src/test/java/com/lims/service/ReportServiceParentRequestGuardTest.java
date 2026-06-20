package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #68 (P10): {@code ReportService.createReport} must
 * verify that the parent {@code Request} exists before inserting a Report.
 * Without this defensive check (or a DB FK), a report can reference a
 * non-existent request — orphan rows.
 *
 * Asserted at source level:
 *   1. createReport loads the parent request via requestMapper
 *   2. throws DATA_NOT_FOUND when the parent is null
 *   3. No Java source references ReportService without the parent check
 */
class ReportServiceParentRequestGuardTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void createReportGuardsOnMissingParent() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/ReportService.java");
        int idx = src.indexOf("public Report createReport");
        assertTrue(idx > 0, "createReport method not found");
        int bodyEnd = Math.min(src.length(), idx + 1500);
        String body = src.substring(idx, bodyEnd);
        boolean loadsParent = body.contains("requestMapper.selectById(requestId)");
        boolean throwsOnMissing = body.contains("DATA_NOT_FOUND")
                && body.contains("parent == null");
        assertTrue(loadsParent && throwsOnMissing,
                "ReportService.createReport must verify the parent request exists " +
                        "and throw DATA_NOT_FOUND when it doesn't, to prevent orphan " +
                        "report rows.");
    }

    @Test
    void noServiceMethodInsertsReportWithoutLoadingParent() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path serviceRoot = p.resolve("lims-service/src/main/java");
            if (!Files.isDirectory(serviceRoot)) continue;
            try (Stream<Path> stream = Files.walk(serviceRoot)) {
                stream.filter(f -> f.toString().endsWith(".java"))
                        .filter(f -> f.toString().contains("/service/"))
                        .forEach(f -> {
                            try {
                                String src = Files.readString(f);
                                if (src.contains("createReport")
                                        && !src.contains("requestMapper")) {
                                    // createReport() must reference requestMapper
                                    assertFalse(true,
                                            f + " has createReport() but no requestMapper — " +
                                                    "orphan check missing");
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            break;
        }
    }
}
