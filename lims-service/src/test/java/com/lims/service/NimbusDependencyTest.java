package com.lims.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NimbusDependencyTest {
    @Test
    void limsServicePomDeclaresNimbusJoseJwt() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("lims-service/pom.xml");
            if (Files.isRegularFile(candidate)) {
                String content = Files.readString(candidate);
                assertTrue(content.contains("com.nimbusds")
                                && content.contains("nimbus-jose-jwt"),
                        "lims-service/pom.xml must declare nimbus-jose-jwt explicitly. " +
                                "Today AuthService.java imports com.nimbusds.jwt.* " +
                                "but the dep is only pulled transitively via the " +
                                "oauth2-resource-server starter in lims-web — fragile.");
                return;
            }
        }
        throw new IllegalStateException("lims-service/pom.xml not found above " + userDir);
    }
}
