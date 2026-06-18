package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayConfigExplicitTest {
    @Test
    void applicationYamlDeclaresFlywayEnabled() throws Exception {
        String content = read();
        // Match `spring:` block containing `flyway:` block with `enabled: true`.
        // Allow either flat `spring.flyway.enabled: true` or nested YAML.
        boolean hasFlat = content.contains("spring.flyway.enabled: true");
        boolean hasNested = Pattern.compile(
                "(?ms)^\\s*spring:\\s*$.*?^\\s*flyway:\\s*$.*?^\\s*enabled:\\s*true\\s*$"
        ).matcher(content).find();
        assertTrue(hasFlat || hasNested,
                "application.yml must explicitly enable Flyway. Today the value " +
                        "is implicit via Spring Boot autoconfig default; issue #31 wants " +
                        "it explicit so a reader doesn't have to guess.");
    }

    private static String read() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path c = p.resolve("lims-web/src/main/resources/application.yml");
            if (Files.isRegularFile(c)) return Files.readString(c);
        }
        throw new IllegalStateException("application.yml not found");
    }
}
