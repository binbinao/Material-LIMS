package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProTableErrorSurfacingTest {
    @Test
    void everyProTableCatchBlockSurfacesError() throws Exception {
        String[] files = {
            "lims-web-ui/src/pages/request/RequestList/index.tsx",
            "lims-web-ui/src/pages/report/ReportList/index.tsx",
            "lims-web-ui/src/pages/admin/LogList/index.tsx",
            "lims-web-ui/src/pages/equipment/EquipmentRepairs/index.tsx",
            "lims-web-ui/src/pages/test-data/AnalysisItemList/index.tsx",
            "lims-web-ui/src/pages/basic-data/BrandList/index.tsx",
            "lims-web-ui/src/pages/basic-data/RequestTypeList/index.tsx",
            "lims-web-ui/src/pages/basic-data/HolidayList/index.tsx",
        };
        for (String rel : files) {
            String content = read(rel);
            assertTrue(content.contains("message.error") && content.contains("Load failed"),
                    rel + " must call message.error() in its ProTable catch " +
                            "block so the user sees 'Load failed' instead of an empty table.");
        }
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
