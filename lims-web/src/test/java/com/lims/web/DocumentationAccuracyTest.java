package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #32: documentation drift between code and CLAUDE.md.
 * The most-confusing drift was the @AuditLog → "audit_log" claim,
 * when the real table is sys_operation_log.
 */
class DocumentationAccuracyTest {

    @Test
    void claudeMdMentionsSysOperationLogNotAuditLog() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path claudeMd = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("CLAUDE.md");
            if (Files.isRegularFile(candidate)) {
                claudeMd = candidate;
                break;
            }
        }
        assertTrue(claudeMd != null, "CLAUDE.md not found above " + userDir);
        String content = Files.readString(claudeMd);
        assertTrue(content.contains("sys_operation_log"),
                "CLAUDE.md must reference sys_operation_log (the table AuditLogAspect " +
                        "actually writes to), not the invented 'audit_log'.");
    }
}
