package com.lims.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #36 (L-7 + L-8): two small defects in RequestService.
 *  L-7: assignRequest silently skips tasks not belonging to the request.
 *  L-8: generateRequestNo uses %04d, breaks at 10000.
 */
class RequestServiceSmallFixesTest {

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-service/src/main/java/com/lims/service/RequestService.java");
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException("RequestService.java not found");
    }

    @Test
    void assignRequestValidatesThatTaskBelongsToRequest() throws Exception {
        String content = readSource();
        int idx = content.indexOf("public void assignRequest");
        assertTrue(idx > 0, "assignRequest method not found");
        int bodyEnd = Math.min(content.length(), idx + 1500);
        String body = content.substring(idx, bodyEnd);
        boolean hasExplicitCheck = body.contains("task.getRequestId()")
                && (body.contains("!=") || body.contains(".equals(requestId)"));
        boolean hasThrowOnMismatch = body.contains("BusinessException")
                || body.contains("throw ");
        assertTrue(hasExplicitCheck && hasThrowOnMismatch,
                "RequestService.assignRequest must throw BusinessException " +
                        "when an assignment taskId does not belong to the requestId.");
    }

    @Test
    void generateRequestNoHandlesCounterOverflow() throws Exception {
        String content = readSource();
        int idx = content.indexOf("private String generateRequestNo()");
        assertTrue(idx > 0, "generateRequestNo method not found");
        int bodyEnd = Math.min(content.length(), idx + 1000);
        String body = content.substring(idx, bodyEnd);
        boolean hasWideFormat = body.contains("\"%05d\"")
                || body.contains("\"%06d\"")
                || body.contains("\"%07d\"")
                || body.contains("\"%08d\"");
        assertTrue(hasWideFormat,
                "RequestService.generateRequestNo must use %05d or wider so the " +
                        "REQ-YYYY-NNNN shape stays consistent past counter 9999.");
    }
}
