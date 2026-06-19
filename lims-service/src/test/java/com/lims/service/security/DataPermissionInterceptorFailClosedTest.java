package com.lims.service.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aligned with issue #19: {@code DataPermissionInterceptor} uses fail-soft
 * (regex fallback) instead of fail-closed (throw). jsqlparser 4.9 cannot
 * parse JOIN/UNION/CTE/subquery statements, and a hard throw used to break
 * every legitimate complex query for non-ADMIN/MANAGER users.
 *
 * The current implementation:
 *   1. tries a regex-based fallback that picks the first FROM table and
 *      adds the outer-row filter
 *   2. if that also fails, logs a WARN and lets the original SQL through
 *
 * This test asserts both: the catch block does NOT throw, AND a regex
 * fallback path exists.
 */
class DataPermissionInterceptorFailClosedTest {

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
    void dataPermissionInterceptorFailsSoftOnParseFailure() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/security/DataPermissionInterceptor.java");
        int catchIdx = content.indexOf("catch (Exception e)");
        assertNotEquals(-1, catchIdx, "catch (Exception e) block not found in beforeQuery");
        int blockEnd = content.indexOf("\n    }", catchIdx);
        assertNotEquals(-1, blockEnd, "catch block end not found");
        String block = content.substring(catchIdx, blockEnd);
        // After issue #19, the catch block uses fail-soft + regex fallback.
        // No `throw new ...` should appear in this catch.
        assertTrue(!block.contains("throw new BusinessException")
                        && !block.contains("throw new RuntimeException"),
                "DataPermissionInterceptor.beforeQuery's catch block must NOT " +
                        "throw — issue #19 changed fail-closed to fail-soft with " +
                        "regex fallback. A throw here would break every legitimate " +
                        "complex query (JOIN/UNION/CTE) for non-ADMIN/MANAGER users.");
        // The regex fallback path must exist.
        assertTrue(content.contains("tryRegexFallback") || content.contains("regex"),
                "DataPermissionInterceptor must have a regex-based fallback path " +
                        "for queries jsqlparser cannot parse.");
    }
}
