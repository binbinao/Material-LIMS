package com.lims.web;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #34: the basic-data and test-data navigation
 * menu advertises routes to AnalysisTypeList, SpecificationList,
 * TestGroupList, TestSiteList, and RequestNoteList — but each
 * renders only an empty ProTable stub. The fix is to remove the
 * routes and the page files.
 */
class PlaceholderPagesRemovedTest {

    private static final List<String> PLACEHOLDER_PAGES = List.of(
            "/test-data/analysis-types",
            "/test-data/specifications",
            "/test-data/groups",
            "/test-data/sites",
            "/basic-data/request-notes"
    );

    @Test
    void noRoutePointsToPlaceholderPages() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path routes = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("lims-web-ui/config/routes.ts");
            if (Files.isRegularFile(candidate)) { routes = candidate; break; }
        }
        assertTrue(routes != null, "lims-web-ui/config/routes.ts not found");
        String content = Files.readString(routes);
        for (String path : PLACEHOLDER_PAGES) {
            assertFalse(content.contains("path: '" + path),
                    "routes.ts still references placeholder path " + path);
        }
    }

    @Test
    void noPlaceholderPageFileHasEmptyTableStub() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (String dir : List.of(
                "lims-web-ui/src/pages/basic-data/RequestNoteList",
                "lims-web-ui/src/pages/test-data/AnalysisTypeList",
                "lims-web-ui/src/pages/test-data/SpecificationList",
                "lims-web-ui/src/pages/test-data/TestGroupList",
                "lims-web-ui/src/pages/test-data/TestSiteList")) {
            Path pageDir = null;
            for (Path p = userDir; p != null; p = p.getParent()) {
                Path candidate = p.resolve(dir);
                if (Files.isDirectory(candidate)) { pageDir = candidate; break; }
            }
            if (pageDir == null) continue;
            try (var walk = Files.walk(pageDir)) {
                for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                    if (!file.getFileName().toString().endsWith(".tsx")) continue;
                    String content = Files.readString(file);
                    if (content.contains("data: [], total: 0, success: true")) {
                        throw new AssertionError(dir + " still contains the empty-table stub");
                    }
                }
            }
        }
    }
}
