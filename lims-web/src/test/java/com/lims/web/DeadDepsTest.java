package com.lims.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #33: dead dependencies in
 * {@code lims-web-ui/package.json}. These packages are declared
 * but have zero {@code import} references in {@code src/}.
 */
class DeadDepsTest {

    private static final List<String> DEAD_DEPS = List.of(
            "i18next",
            "i18next-browser-languagedetector",
            "react-i18next",
            "numeral",
            "umi-request",
            "classnames",
            "lodash"
    );

    private static String readPackageJson() throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("lims-web-ui/package.json");
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IOException("lims-web-ui/package.json not found above " + userDir);
    }

    private static boolean srcImports(String dep) {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path srcRoot = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("lims-web-ui/src");
            if (Files.isDirectory(candidate)) {
                srcRoot = candidate;
                break;
            }
        }
        if (srcRoot == null) return false;
        Pattern p = Pattern.compile(
                "(?:from\\s+['\"]" + Pattern.quote(dep) + "['\"]"
                        + "|require\\s*\\(\\s*['\"]" + Pattern.quote(dep) + "['\"])");
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String name = file.getFileName().toString();
                if (!(name.endsWith(".ts") || name.endsWith(".tsx")
                        || name.endsWith(".js") || name.endsWith(".jsx"))) {
                    continue;
                }
                String content = Files.readString(file);
                if (p.matcher(content).find()) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Test
    void packageJsonHasNoDeadRuntimeDependencies() throws Exception {
        String pkg = readPackageJson();
        for (String dep : DEAD_DEPS) {
            boolean declared = pkg.contains("\"" + dep + "\":");
            assertFalse(declared,
                    "package.json declares " + dep + " but no source file in " +
                            "lims-web-ui/src/ imports it. Remove the dead dependency.");
        }
    }

    @Test
    void srcDoesNotImportDeadDependencies() {
        for (String dep : DEAD_DEPS) {
            boolean imported = srcImports(dep);
            assertFalse(imported,
                    "lims-web-ui/src/ imports " + dep + " but it is not declared " +
                            "in package.json. Either add the dep or remove the import.");
        }
    }

    @Test
    void deadDepListIsNotEmpty() {
        assertTrue(!DEAD_DEPS.isEmpty());
        assertTrue(DEAD_DEPS.size() >= 5);
    }
}
