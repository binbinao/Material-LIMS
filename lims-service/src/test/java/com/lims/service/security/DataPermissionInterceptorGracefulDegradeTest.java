package com.lims.service.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #19 (post-fix review M-7): the catch block in
 * {@code DataPermissionInterceptor} throws BusinessException on
 * jsqlparser parse failure, which breaks every JOIN/UNION/CTE query for
 * non-ADMIN users.
 */
class DataPermissionInterceptorGracefulDegradeTest {

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
    void catchBlockDoesNotRethrowBusinessException() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/security/DataPermissionInterceptor.java");
        int catchIdx = content.indexOf("catch (Exception e)");
        assertNotEquals(-1, catchIdx, "catch block not found");
        int blockEnd = -1;
        int depth = 0;
        for (int i = catchIdx; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { blockEnd = i; break; }
            }
        }
        String block = content.substring(catchIdx, blockEnd);
        boolean stillThrows = block.contains("throw new BusinessException");
        assertTrue(!stillThrows,
                "catch block must NOT throw BusinessException on parse failure.");
    }

    @Test
    void catchBlockLogsWarningForFailSoft() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/security/DataPermissionInterceptor.java");
        int catchIdx = content.indexOf("catch (Exception e)");
        int blockEnd = -1;
        int depth = 0;
        for (int i = catchIdx; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { blockEnd = i; break; }
            }
        }
        String block = content.substring(catchIdx, blockEnd);
        boolean hasLog = block.contains("log.warn") || block.contains("log.error")
                || block.contains("log.info");
        assertTrue(hasLog,
                "The fail-soft catch block must log a warning.");
    }
}