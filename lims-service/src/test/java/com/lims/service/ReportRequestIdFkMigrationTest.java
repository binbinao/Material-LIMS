package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #68 (P10): the {@code report.request_id} column has
 * no foreign key — orphan reports are possible if data is inserted
 * directly (or via a code path that bypasses the defensive check added
 * in P5). This test asserts that a Flyway V5 migration re-adds the FK.
 *
 * Note: P5 already added a defensive {@code requestMapper.selectById}
 * guard inside {@code ReportService.createReport}, so the code path is
 * protected. The DB-level FK is defense-in-depth.
 */
class ReportRequestIdFkMigrationTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void flywayV5MigrationAddsReportRequestFk() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path[] found = new Path[1];
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path migrationDir = p.resolve(
                    "lims-web/src/main/resources/db/migration");
            if (Files.isDirectory(migrationDir)) {
                try (Stream<Path> stream = Files.list(migrationDir)) {
                    stream.filter(f -> f.getFileName().toString().startsWith("V5"))
                            .findFirst()
                            .ifPresent(f -> found[0] = f);
                }
                break;
            }
        }
        assertTrue(found[0] != null,
                "Flyway V5 migration adding report.request_id FK must exist");
        String mig = Files.readString(found[0]);
        assertTrue(mig.contains("request_id"),
                "V5 migration must reference request_id column");
        assertTrue(mig.contains("REFERENCES"),
                "V5 migration must add a REFERENCES constraint");
        assertTrue(mig.contains("report"),
                "V5 migration must target the report table");
        assertTrue(mig.contains("request"),
                "V5 migration must reference the request table");
    }
}
