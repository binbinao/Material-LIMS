package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #18 (post-fix review arch H-1): the project ships
 * {@code init.sql} as a Postgres docker-entrypoint mount, but it only
 * runs on the first creation of the {@code pgdata} volume. Once Flyway
 * is enabled (via fix #7) any production environment with an existing
 * DB never seeds.
 *
 * Three contracts the fix must satisfy (asserted at source level):
 *
 *  1. A Flyway {@code V2__seed_*.sql} migration lives at
 *     {@code lims-web/src/main/resources/db/migration/} so the seed
 *     data ships through Flyway and runs on every fresh deployment.
 *  2. The V2 migration must contain real seed data (not be a no-op).
 *  3. {@code application-dev.yml} must not disable Flyway in a way that
 *     leaves dev without seed data.
 */
class FlywaySeedTest {

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
    void v2SeedMigrationExists() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-web/src/main/resources/db/migration/V2__seed_dev.sql");
            if (Files.isRegularFile(candidate)) {
                return; // found
            }
        }
        assertTrue(false,
                "lims-web/src/main/resources/db/migration/V2__seed_dev.sql must " +
                        "exist so seed data ships through Flyway.");
    }

    @Test
    void seedMigrationContainsBrandSeedData() throws Exception {
        String content = readSource(
                "lims-web/src/main/resources/db/migration/V2__seed_dev.sql");
        boolean hasBrandInsert = content.contains("INSERT INTO brand")
                || content.contains("insert into brand");
        assertTrue(hasBrandInsert,
                "V2__seed_dev.sql must contain INSERT INTO brand statements.");
    }

    @Test
    void applicationDevConfigDoesNotSilentlyDisableFlyway() throws Exception {
        String content = readSource(
                "lims-web/src/main/resources/application-dev.yml");
        boolean disablesFlyway = content.contains("spring.flyway.enabled: false");
        assertTrue(!disablesFlyway,
                "application-dev.yml must not set spring.flyway.enabled=false.");
    }
}