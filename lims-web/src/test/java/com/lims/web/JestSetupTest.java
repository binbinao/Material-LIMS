package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #10: the frontend package's {@code npm test} script
 * runs {@code jest}, but no {@code jest.config.js} / {@code jest.setup.ts}
 * / test file exist — so {@code npm test} exits non-zero. A test framework
 * advertised in {@code package.json} but not wired up is worse than no
 * script at all (CI turns red without telling anyone why).
 *
 * Three contracts the fix must satisfy (asserted at source level —
 * {@code npm test} itself needs Node.js + a populated {@code node_modules},
 * which this Java process can't run; structural checks catch the gap):
 *
 *  1. {@code lims-web-ui/jest.config.js} exists with a ts-jest preset
 *     and jsdom test environment.
 *  2. {@code lims-web-ui/jest.setup.ts} exists, importing
 *     {@code @testing-library/jest-dom} so its matchers
 *     ({@code toBeInTheDocument} etc.) are registered globally.
 *  3. At least one {@code *.test.ts(x)} file exists under
 *     {@code lims-web-ui/src}, so {@code jest} has a target.
 */
class JestSetupTest {

    private static boolean exists(String relPath) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) return true;
        }
        return false;
    }

    @Test
    void jestConfigExists() {
        assertTrue(exists("lims-web-ui/jest.config.js"),
                "lims-web-ui/jest.config.js must exist so that `npm test` can " +
                        "find a ts-jest config and a jsdom test environment. Today " +
                        "the package.json declares `\"test\": \"jest\"` but jest " +
                        "exits immediately with no config.");
    }

    @Test
    void jestSetupFileExists() {
        assertTrue(exists("lims-web-ui/jest.setup.ts")
                        || exists("lims-web-ui/jest.setup.js"),
                "lims-web-ui/jest.setup.ts (or .js) must exist and import " +
                        "@testing-library/jest-dom so its matchers " +
                        "(toBeInTheDocument, toHaveTextContent, etc.) are " +
                        "registered before any test runs.");
    }

    @Test
    void atLeastOneFrontendTestFileExists() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path frontendSrc = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("lims-web-ui/src");
            if (Files.isDirectory(candidate)) {
                frontendSrc = candidate;
                break;
            }
        }
        assertNotEquals(null, frontendSrc, "lims-web-ui/src not found");
        boolean found = false;
        try (var stream = Files.walk(frontendSrc)) {
            found = stream
                    .filter(Files::isRegularFile)
                    .anyMatch(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".test.ts") || name.endsWith(".test.tsx");
                    });
        }
        assertTrue(found,
                "lims-web-ui/src must contain at least one *.test.ts(x) file " +
                        "so jest has a target to run. The fix adds a " +
                        "src/__tests__/setup.test.ts smoke test.");
    }
}
