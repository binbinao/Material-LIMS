package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleGateActionButtonsTest {
    @Test
    void requestDetailUsesAccessForActionButtons() throws Exception {
        String content = read("lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        assertTrue(content.contains("useAccess()"),
                "RequestDetail must call useAccess() to gate action buttons by role.");
    }

    @Test
    void reportDetailUsesAccessForActionButtons() throws Exception {
        String content = read("lims-web-ui/src/pages/report/ReportDetail/index.tsx");
        assertTrue(content.contains("useAccess()"),
                "ReportDetail must call useAccess() to gate action buttons by role.");
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
