package com.lims.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericErrorMessagesTest {

    @Test
    void authServiceStateMismatchMessageIsGeneric() throws Exception {
        String content = readSource();
        // Find the throw BusinessException line that follows the
        // "state mismatch" check.
        int stateIdx = content.indexOf("state mismatch");
        assertTrue(stateIdx > 0, "state mismatch check not found");
        // Locate the next throw line after stateIdx.
        int throwIdx = content.indexOf("throw new BusinessException", stateIdx);
        assertTrue(throwIdx > 0, "throw not found after state mismatch check");
        int lineEnd = content.indexOf(";", throwIdx);
        String throwLine = content.substring(throwIdx, lineEnd);
        // The throw line must contain only a static message — no string
        // concatenation of expected / actual values.
        boolean concatenatesExpected = throwLine.contains("+") && (
                throwLine.contains("expectedState") || throwLine.contains("expected ")
                || throwLine.contains("state"));
        assertTrue(!concatenatesExpected,
                "state-mismatch throw message must be a static string. Got: " + throwLine);
    }

    @Test
    void authServiceNonceMismatchMessageIsGeneric() throws Exception {
        String content = readSource();
        int nonceIdx = content.indexOf("nonce mismatch");
        assertTrue(nonceIdx > 0, "nonce mismatch check not found");
        int throwIdx = content.indexOf("throw new BusinessException", nonceIdx);
        assertTrue(throwIdx > 0, "throw not found after nonce mismatch check");
        int lineEnd = content.indexOf(";", throwIdx);
        String throwLine = content.substring(throwIdx, lineEnd);
        boolean concatenatesExpected = throwLine.contains("+") && (
                throwLine.contains("expectedNonce") || throwLine.contains("expected ")
                || throwLine.contains("nonce"));
        assertTrue(!concatenatesExpected,
                "nonce-mismatch throw message must be a static string. Got: " + throwLine);
    }

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-service/src/main/java/com/lims/service/AuthService.java");
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException("AuthService.java not found");
    }
}
