package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #60 (P1): {@code ReportService.getRevisions} must
 * filter by {@code request_id}, not {@code id}. Today it filters by id —
 * returning only the report itself instead of the full version history
 * for the request.
 *
 * The fix: load the report by id, get its {@code requestId}, then query
 * all reports where {@code request_id} matches, ordered by version desc.
 *
 * Asserted at source level — a full integration test would need a
 * Postgres fixture with rows in {@code report}.
 */
class ReportServiceRevisionsByRequestTest {

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
    void getRevisionsFiltersByRequestId() throws Exception {
        String src = readSource();
        int idx = src.indexOf("public List<Report> getRevisions");
        assertTrue(idx > 0, "getRevisions method not found");
        int bodyEnd = Math.min(src.length(), idx + 1200);
        String body = src.substring(idx, bodyEnd);
        boolean loadsById = body.contains("reportMapper.selectById(reportId)")
                || (body.contains("selectById") && body.contains("getRequestId"));
        boolean queriesByRequestId = body.contains("getRequestId")
                && body.contains("eq(Report::getRequestId");
        assertTrue(loadsById && queriesByRequestId,
                "ReportService.getRevisions must first load the report by " +
                        "id to obtain its requestId, then query all reports " +
                        "where request_id equals that requestId. " +
                        "Today it filters by report.id, returning only 1 row.");
    }
}
