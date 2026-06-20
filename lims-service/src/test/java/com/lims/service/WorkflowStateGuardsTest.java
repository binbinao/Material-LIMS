package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD / source-level tests for review findings H1, H2, H5.
 *
 *  H1 — completeRequest must refuse non-APPROVING source state
 *  H2 — rejectRequest must refuse re-flipping a terminal state (COMPLETED/REJECTED)
 *  H5 — rejectReport must enforce four-eyes (manager != author)
 *
 * Asserted at source level so the tests run without a Spring context
 * (no live DB, no Flowable). If the regression returns, the test fails.
 */
class WorkflowStateGuardsTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    private static String methodBody(String source, String signatureStart, int maxLen) {
        int idx = source.indexOf(signatureStart);
        assertTrue(idx > 0, signatureStart + " not found");
        return source.substring(idx, Math.min(source.length(), idx + maxLen));
    }

    // ─── H1 ────────────────────────────────────────────────────────────

    @Test
    void completeRequestGuardsOnNonApprovingState() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        String body = methodBody(src, "public void completeRequest", 1500);
        boolean hasStateGuard = body.contains("APPROVING.getValue().equals(request.getStatus())")
                && body.contains("REQUEST_STATUS_INVALID");
        assertTrue(hasStateGuard,
                "RequestService.completeRequest must reject when status != APPROVING " +
                        "and throw REQUEST_STATUS_INVALID (review H1).");
    }

    @Test
    void completeRequestHasServiceRoleGuard() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        String body = methodBody(src, "public void completeRequest", 1500);
        assertTrue(body.contains("requireRequestRole("),
                "RequestService.completeRequest must call requireRequestRole for " +
                        "defense-in-depth (review H1).");
    }

    // ─── H2 ────────────────────────────────────────────────────────────

    @Test
    void rejectRequestGuardsOnTerminalStates() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        String body = methodBody(src, "public void rejectRequest", 1500);
        boolean blocksCompleted = body.contains("COMPLETED.getValue().equals(current)");
        boolean blocksAlreadyRejected = body.contains("REJECTED.getValue().equals(current)");
        boolean throwsOnTerminal = body.contains("REQUEST_STATUS_INVALID")
                && body.contains("terminal state");
        assertTrue(blocksCompleted && blocksAlreadyRejected && throwsOnTerminal,
                "RequestService.rejectRequest must refuse re-flipping a COMPLETED or " +
                        "already-REJECTED request (review H2).");
    }

    @Test
    void rejectRequestHasServiceRoleGuard() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        String body = methodBody(src, "public void rejectRequest", 1500);
        assertTrue(body.contains("requireRequestRole("),
                "RequestService.rejectRequest must call requireRequestRole for " +
                        "defense-in-depth (review H2).");
    }

    // ─── H5 ────────────────────────────────────────────────────────────

    @Test
    void rejectReportEnforcesFourEyes() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/ReportService.java");
        String body = methodBody(src, "public void rejectReport", 2000);
        boolean mirrorsApprove = body.contains("getAuthorId().equals(managerId)")
                || body.contains("getAuthorId() != null && report.getAuthorId().equals(managerId)");
        assertTrue(mirrorsApprove,
                "ReportService.rejectReport must mirror the four-eyes check from " +
                        "approveReport — the author may not reject their own report (review H5).");
    }

    // ─── Helper existence ─────────────────────────────────────────────

    @Test
    void requireRequestRoleHelperExists() throws Exception {
        String src = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        assertTrue(src.contains("private void requireRequestRole("),
                "RequestService.requireRequestRole(String...) helper must exist for " +
                        "the H1/H2 service-layer role guards.");
    }
}
