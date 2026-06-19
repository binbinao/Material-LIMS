package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #66 (P9): the {@code Report} entity's {@code taskId}
 * field and the {@code report.task_id} column are dead schema — no service
 * ever writes them. This test asserts:
 *
 *   1. Report entity does NOT declare taskId
 *   2. A Flyway V4 migration drops task_id from the report table
 *   3. No Java source still references Report.taskId (except the test itself)
 */
class ReportTaskIdRemovedTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void reportEntityOmitsTaskIdField() throws Exception {
        String src = readSource(
                "lims-model/src/main/java/com/lims/model/entity/Report.java");
        assertFalse(src.contains("private String taskId"),
                "Report entity must NOT declare private String taskId — " +
                        "the field is dead schema, no service writes it.");
    }

    @Test
    void flywayV4MigrationDropsTaskId() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path[] found = new Path[1];
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path migrationDir = p.resolve(
                    "lims-web/src/main/resources/db/migration");
            if (Files.isDirectory(migrationDir)) {
                try (Stream<Path> stream = Files.list(migrationDir)) {
                    stream.filter(f -> f.getFileName().toString().startsWith("V4"))
                            .findFirst()
                            .ifPresent(f -> found[0] = f);
                }
                break;
            }
        }
        assertTrue(found[0] != null,
                "Flyway V4 migration dropping task_id must exist");
        String mig = Files.readString(found[0]);
        assertTrue(mig.contains("task_id"),
                "V4 migration must drop task_id column");
        assertTrue(mig.contains("report"),
                "V4 migration must target the report table");
    }

    @Test
    void noJavaSourceReferencesReportTaskId() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path limsServiceMain = p.resolve("lims-service/src/main/java");
            Path limsWebMain = p.resolve("lims-web/src/main/java");
            Path limsDaoMain = p.resolve("lims-dao/src/main/java");
            for (Path root : new Path[]{limsServiceMain, limsWebMain, limsDaoMain}) {
                if (!Files.isDirectory(root)) continue;
                try (Stream<Path> stream = Files.walk(root)) {
                    stream.filter(f -> f.toString().endsWith(".java"))
                            .forEach(f -> {
                                try {
                                    String src = Files.readString(f);
                                    assertFalse(src.contains("setTaskId") || src.contains("getTaskId"),
                                            f + " still references Report.taskId");
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
            break;
        }
    }
}
