package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #7: {@code schema.sql} declares tables that
 * {@code REFERENCES sys_user(id)} before {@code CREATE TABLE sys_user}
 * itself, so applying the schema to a fresh database fails with
 * {@code relation "sys_user" does not exist}. Additionally the project
 * has no migration tool — only a manually-runnable {@code schema.sql},
 * so a second schema change has no clean upgrade path.
 *
 * Three contracts the fix must satisfy (asserted at source level —
 * a full integration test would need a Testcontainers Postgres, which
 * the project does not yet have):
 *
 *  1. In {@code schema.sql}, the {@code CREATE TABLE sys_user} statement
 *     appears BEFORE every {@code REFERENCES sys_user(id)} clause. This
 *     fixes the forward-reference startup failure.
 *  2. {@code lims-web/pom.xml} declares the {@code flyway-core}
 *     dependency, so future schema changes can ship as versioned
 *     migrations instead of overwriting {@code schema.sql}.
 *  3. A Flyway migration script lives at the conventional
 *     {@code db/migration/V1__init.sql} path so the {@code spring-boot}
 *     autoconfig picks it up out of the classpath.
 */
class SchemaOrderAndFlywayTest {

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
    void schemaSqlCreatesSysUserBeforeAnyTableReferencesIt() throws Exception {
        String content = readSource(
                "lims-web/src/main/resources/db/schema.sql");
        int sysUserIdx = content.indexOf("CREATE TABLE sys_user");
        assertNotEquals(-1, sysUserIdx,
                "CREATE TABLE sys_user not found in schema.sql");
        // Find every REFERENCES sys_user(id) clause.
        Pattern p = Pattern.compile("REFERENCES\\s+sys_user\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        while (m.find()) {
            int refIdx = m.start();
            assertTrue(refIdx > sysUserIdx,
                    "schema.sql has REFERENCES sys_user(id) at line/col " + refIdx +
                            " BEFORE the CREATE TABLE sys_user at " + sysUserIdx +
                            ". PostgreSQL will refuse to apply this schema because " +
                            "sys_user does not yet exist when those tables are created.");
        }
    }

    @Test
    void limsWebPomDeclaresFlywayDependency() throws Exception {
        String content = readSource("lims-web/pom.xml");
        assertTrue(content.contains("flyway-core"),
                "lims-web/pom.xml must declare the flyway-core dependency so " +
                        "Spring Boot's Flyway autoconfig can pick up db/migration/*.sql. " +
                        "Today there is no migration tool — schema changes require " +
                        "manual psql -f and have no upgrade path.");
    }

    @Test
    void flywayV1InitScriptExists() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path script = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-web/src/main/resources/db/migration/V1__init.sql");
            if (Files.isRegularFile(candidate)) {
                script = candidate;
                break;
            }
        }
        assertTrue(script != null,
                "lims-web/src/main/resources/db/migration/V1__init.sql must exist so " +
                        "Spring Boot Flyway autoconfig picks it up. Today schema lives at " +
                        "lims-web/src/main/resources/db/schema.sql, which Flyway does not " +
                        "discover unless classpath:db/migration is wired manually.");
    }
}
