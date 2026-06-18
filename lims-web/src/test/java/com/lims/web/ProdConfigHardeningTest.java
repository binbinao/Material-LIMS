package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdConfigHardeningTest {
    @Test
    void prodApplicationYamlDoesNotEnableSecurityDebug() throws Exception {
        String content = read("lims-web/src/main/resources/application.yml");
        // Issue #26: prod profile must NOT default to DEBUG for spring-security.
        assertFalse(content.contains("org.springframework.security: DEBUG"),
                "application.yml must not enable org.springframework.security DEBUG " +
                        "in the default (prod) profile.");
    }

    @Test
    void prodApplicationYamlDoesNotDefaultMinioAdmin() throws Exception {
        String content = read("lims-web/src/main/resources/application.yml");
        // Issue #27: prod profile must not hardcode minioadmin as default.
        assertFalse(content.contains("MINIO_ACCESS_KEY:minioadmin") || content.contains("minioaccess")
                        && content.contains(":minioadmin"),
                "application.yml must not default MINIO_ACCESS_KEY to minioadmin " +
                        "in the prod profile — use an empty default that fails fast.");
    }

    @Test
    void webConfigCorsReadsFromProperty() throws Exception {
        String content = read("lims-web/src/main/java/com/lims/web/config/WebConfig.java");
        // Issue #28: CORS must read allowed origins from a property, not
        // hardcode localhost.
        assertTrue(content.contains("cors") && (
                content.contains("${cors.allowed-origins") || content.contains("@Value")),
                "WebConfig.java must externalize CORS allowed origins via @Value " +
                        "(${cors.allowed-origins:http://localhost:8000} or similar).");
    }

    private static String read(String rel) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path c = p.resolve(rel);
            if (Files.isRegularFile(c)) return Files.readString(c);
        }
        throw new IllegalStateException(rel + " not found");
    }
}
