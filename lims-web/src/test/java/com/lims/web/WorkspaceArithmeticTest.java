package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #12: the Workspace dashboard's "Active Requests"
 * Statistic uses
 *   {@code value={requestStats.SUBMITTED || 0 + requestStats.ASSIGNED || 0}}
 * Because {@code +} binds tighter than {@code ||}, this evaluates as
 * {@code requestStats.SUBMITTED || (0 + requestStats.ASSIGNED) || 0}
 * which is just {@code requestStats.SUBMITTED} — the ASSIGNED count is
 * silently dropped. The card's number is wrong, and clicking it
 * (which routes to {@code /request/kanban}) misleads the user.
 *
 * The fix is to parenthesize both operands so the intent
 * {@code (A || 0) + (B || 0)} is unambiguous.
 */
class WorkspaceArithmeticTest {

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
    void activeRequestsStatisticParenthesizesBothOperands() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/Workspace/index.tsx");
        boolean hasParens = content.contains("(requestStats.SUBMITTED || 0)")
                && content.contains("(requestStats.ASSIGNED || 0)");
        assertTrue(hasParens,
                "Workspace's 'Active Requests' Statistic must parenthesize each " +
                        "operand: value={(requestStats.SUBMITTED || 0) + " +
                        "(requestStats.ASSIGNED || 0)}. Today the expression is " +
                        "requestStats.SUBMITTED || 0 + requestStats.ASSIGNED || 0, " +
                        "which JavaScript parses as (SUBMITTED) || (0 + ASSIGNED) || 0, " +
                        "silently dropping the ASSIGNED count.");
    }
}
