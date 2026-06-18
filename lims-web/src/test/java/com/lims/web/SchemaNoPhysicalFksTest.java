package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #8: {@code schema.sql} declares 21+
 * {@code REFERENCES table(col)} foreign keys, but {@code CLAUDE.md},
 * {@code CODEBUDDY.md}, and {@code docs/design/material-lims-design.md}
 * all say "no physical foreign keys — enforced at the application layer".
 *
 * This contradiction is a real bug: a developer reading the docs will
 * believe there are no FKs, write code that doesn't worry about
 * referential integrity, and then be confused when a row in
 * {@code request} can't be inserted because {@code sys_user} has no row
 * with the same id.
 *
 * The fix picks one side and aligns. The {@code schema.sql} is the
 * source of truth at runtime; the docs are prose. This test asserts
 * that {@code schema.sql} matches the docs (i.e. carries zero physical
 * FKs) — referential integrity is enforced by the application layer
 * ({@code DataPermissionInterceptor}, soft-delete via {@code deleted_at},
 * the {@code MyBatisMetaObjectHandler}-filled {@code created_by/updated_by},
 * and the {@code RequestService.findOrCreateUser} onboarding path).
 */
class SchemaNoPhysicalFksTest {

    private static final Pattern REFERENCES_CLAUSE = Pattern.compile(
            "\\bREFERENCES\\s+\\w+\\s*\\([^)]*\\)");

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
    void schemaSqlHasNoPhysicalForeignKeys() throws Exception {
        String content = readSource(
                "lims-web/src/main/resources/db/schema.sql");
        int count = 0;
        Matcher m = REFERENCES_CLAUSE.matcher(content);
        while (m.find()) count++;
        assertEquals(0, count,
                "schema.sql must have 0 physical foreign-key REFERENCES clauses to " +
                        "match the design docs (CLAUDE.md, CODEBUDDY.md, " +
                        "docs/design/material-lims-design.md), which all say \"no " +
                        "physical foreign keys, enforced at the application layer\". " +
                        "Today the file declares " + count + " REFERENCES clauses; " +
                        "either the docs or the schema is wrong. The application " +
                        "layer (DataPermissionInterceptor + soft-delete on " +
                        "deleted_at + RequestService.findOrCreateUser) already " +
                        "covers referential integrity, so drop the REFERENCES.");
    }

    @Test
    void docsAgreeOnNoPhysicalForeignKeys() throws Exception {
        for (String relPath : new String[]{
                "CLAUDE.md",
                "CODEBUDDY.md",
                "docs/design/material-lims-design.md"}) {
            String content = readSource(relPath);
            assertTrue(
                    content.contains("不建物理外键")
                            || content.contains("物理外键不创建")
                            || content.contains("no physical foreign key")
                            || content.contains("no physical foreign keys")
                            || content.contains("无物理外键"),
                    relPath + " should state that there are no physical " +
                            "foreign keys (referential integrity is enforced at the " +
                            "application layer). Today it doesn't, which is the " +
                            "other half of the issue #8 doc/schema drift.");
        }
    }
}
