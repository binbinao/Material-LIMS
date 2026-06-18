package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #20: updateAnalysisTask auto-transitions request
 * to APPROVING without role-gating. Per post-fix review M-9, a
 * TECHNICAL/ENGINEER assignee can bypass the manager's manual review
 * by marking all their tasks COMPLETED.
 */
class RequestServiceAutoApprovalGateTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void autoTransitionToApprovingIsRoleGated() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/RequestService.java");
        int approvingIdx = content.indexOf("RequestStatus.APPROVING");
        assertTrue(approvingIdx > 0, "APPROVING transition not found");
        int windowStart = Math.max(0, approvingIdx - 600);
        String window = content.substring(windowStart, approvingIdx + 200);
        boolean hasRoleCheck = window.contains("hasRole")
                || window.contains("hasAnyRole")
                || window.contains("principal.hasRole")
                || window.contains("advanceToApproval");
        assertTrue(hasRoleCheck,
                "RequestService.updateAnalysisTask auto-transitions the request " +
                        "to APPROVING without role check. A TECHNICAL/ENGINEER " +
                        "can force the request into manager-review state.");
    }
}