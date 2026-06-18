package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CleanupTest {
    @Test
    void schemaSqlBakDoesNotExist() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path bak = p.resolve("lims-web/src/main/resources/db/schema.sql.bak");
            assertFalse(Files.isRegularFile(bak),
                    "schema.sql.bak must be deleted (issue #30) so the JAR " +
                            "does not package a stale schema snapshot that could " +
                            "accidentally be picked up by classpath:db/schema.sql " +
                            "lookups if Spring's sql.init.mode ever flips to always.");
        }
    }
}
