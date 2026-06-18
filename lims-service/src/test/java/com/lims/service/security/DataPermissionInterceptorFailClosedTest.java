package com.lims.service.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #6: {@code DataPermissionInterceptor} catches the
 * jsqlparser {@code Exception} and just {@code log.warn}s, then falls through
 * with the original SQL. For a non-ADMIN user that means a complex JOIN /
 * UNION / subquery bypasses row-level filtering entirely.
 *
 * The fix must: when jsqlparser fails to parse, throw (fail closed) for
 * non-ADMIN roles so a maliciously-shaped query cannot exfiltrate rows
 * that the row-level filter would otherwise hide. ADMIN/MANAGER may
 * still bypass the filter (the early-return at the top of
 * {@code beforeQuery}), so the fail-closed throw must be scoped to the
 * non-privileged path that entered the parser.
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
    void dataPermissionInterceptorFailsClosedOnParseFailure() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/security/DataPermissionInterceptor.java");
        // Find the catch block that wraps the jsqlparser call.
        int catchIdx = content.indexOf("catch (Exception e)");
        assertNotEquals(-1, catchIdx, "catch (Exception e) block not found in beforeQuery");
        // Slice from catch to the next `    }` (block end) — the interceptor
        // is short enough that a single `\n    }` after catch reliably marks
        // the end of the catch block.
        int blockEnd = content.indexOf("\n    }", catchIdx);
        assertNotEquals(-1, blockEnd, "catch block end not found");
        String block = content.substring(catchIdx, blockEnd);
        assertTrue(block.contains("throw "),
                "DataPermissionInterceptor.beforeQuery's catch (Exception e) block " +
                        "must throw a BusinessException to fail closed. Today it only " +
                        "log.warns and falls through with the original SQL — a non-ADMIN " +
                        "user with a complex JOIN/UNION/subquery bypasses row-level " +
                        "filtering entirely.");
    }
}
