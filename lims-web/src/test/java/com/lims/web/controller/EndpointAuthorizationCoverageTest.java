package com.lims.web.controller;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #16 (post-fix review C-1): with
 * {@code prodFilterChain} ending in {@code .anyRequest().denyAll()},
 * every controller method MUST carry an explicit {@code @PreAuthorize}
 * annotation, or the prod profile will 403 the request. The original
 * PreAuthorizeAndDenyAllTest (#5) checked only ~5 specific endpoints;
 * it missed 18+ GET endpoints that have no annotation.
 *
 * This test walks every controller under
 * {@code lims-web/src/main/java/com/lims/web/controller/} and asserts
 * that each method-level HTTP mapping annotation
 * ({@code @GetMapping / @PostMapping / @PutMapping / @DeleteMapping})
 * is preceded by an {@code @PreAuthorize} line within a 400-character
 * window above the method declaration.
 */
class EndpointAuthorizationCoverageTest {

    private static Path controllersRoot() throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-web/src/main/java/com/lims/web/controller");
            if (Files.isDirectory(candidate)) return candidate;
        }
        throw new IOException("controllers directory not found above " + userDir);
    }

    private static String readAllControllerSources() throws IOException {
        Path root = controllersRoot();
        StringBuilder sb = new StringBuilder();
        Files.list(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals("GlobalExceptionHandler.java"))
                .sorted()
                .forEach(p -> {
                    try {
                        sb.append("\n//FILE=").append(p.getFileName()).append("\n");
                        sb.append(Files.readString(p));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        return sb.toString();
    }

    private static final Pattern HTTP_MAPPING = Pattern.compile(
            "@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\(");

    @TestFactory
    Iterable<DynamicTest> everyEndpointHasPreAuthorize() throws IOException {
        String content = readAllControllerSources();
        List<DynamicTest> tests = new ArrayList<>();

        Matcher m = HTTP_MAPPING.matcher(content);
        while (m.find()) {
            int mapIdx = m.start();
            int publicIdx = content.lastIndexOf("public", mapIdx);
            int windowStart = Math.max(0, mapIdx - 600);  // widen window
            String window = content.substring(windowStart, mapIdx);
            // @PreAuthorize may also appear BETWEEN the HTTP mapping and
            // the method declaration (the common case). Look ahead 200 chars.
            String windowAfter = content.substring(mapIdx,
                    Math.min(content.length(), mapIdx + 200));
            // Also detect class-level @PreAuthorize by finding the file marker
            // for this method's enclosing file, then scanning from the marker
            // up to (but not including) the class declaration.
            int fileMarkerStart = content.lastIndexOf("//FILE=", Math.max(0, publicIdx - 200));
            int fileMarkerEnd = content.indexOf('\n', fileMarkerStart);
            String fileName = content.substring(fileMarkerStart + 7, fileMarkerEnd);
            // Find the start of the class declaration (just after the last
            // class-level annotation before "class "). The class-level
            // @PreAuthorize should be in [fileMarker, classIdx).
            int classIdx = content.indexOf("class ", fileMarkerEnd);
            // The class-level annotations sit between fileMarker and the
            // next "public class" / "class" declaration.
            int classBodyStart = fileMarkerStart;
            int nextClass = content.indexOf("\npublic class ", classBodyStart);
            int nextClass2 = content.indexOf("\nclass ", classBodyStart);
            int classStart = Math.min(
                    nextClass > 0 ? nextClass : Integer.MAX_VALUE,
                    nextClass2 > 0 ? nextClass2 : Integer.MAX_VALUE);
            String classHeader = content.substring(classBodyStart, classStart);
            // Class-level @PreAuthorize applies to all methods in the class.
            boolean classHasPreAuthorize = classHeader.contains("@PreAuthorize");
            // Extract the actual method name from the method signature
            // (public R<X> name(...)) — not the HTTP mapping annotation.
            // Find the open-paren of the METHOD's argument list: walk
            // forward from the mapping's `(` through anything that isn't
            // balanced, then take the next `(`.
            int methodParenIdx = -1;
            int depth = 0;
            for (int i = mapIdx; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '(') {
                    if (depth == 0) {
                        // Remember the mapping's `(` only if we haven't yet
                        // found a deeper one.
                        if (methodParenIdx < 0) methodParenIdx = i;
                        depth++;
                    } else {
                        depth++;
                    }
                } else if (c == ')') {
                    if (depth > 0) depth--;
                    if (depth == 0 && i > methodParenIdx) {
                        methodParenIdx = -1;  // we just closed the mapping; reset
                    }
                }
            }
            // Simpler fallback: scan for "public " then take the identifier
            // before the next '('.
            int methodDeclParen = content.indexOf('(', publicIdx);
            int nameEnd = methodDeclParen;
            while (nameEnd > 0 && Character.isWhitespace(content.charAt(nameEnd - 1))) nameEnd--;
            int nameStart = nameEnd;
            while (nameStart > 0
                    && Character.isJavaIdentifierPart(content.charAt(nameStart - 1))) {
                nameStart--;
            }
            String methodName = content.substring(nameStart, nameEnd);
            String displayName = fileName + " :: " + methodName;

            boolean hasPreAuthorize = window.contains("@PreAuthorize")
                    || windowAfter.contains("@PreAuthorize")
                    || classHasPreAuthorize;
            tests.add(DynamicTest.dynamicTest(displayName,
                    () -> assertTrue(hasPreAuthorize,
                            "HTTP-mapped method " + displayName + " has no @PreAuthorize. " +
                                    "With SecurityConfig.prodFilterChain ending in " +
                                    ".anyRequest().denyAll(), this method returns 403 to " +
                                    "authenticated users in prod. Add @PreAuthorize (e.g. " +
                                    "isAuthenticated() for read, hasRole(...) for write).")));
        }
        return tests;
    }

    @Test
    void noControllerHasZeroPreAuthorize() throws IOException {
        Path root = controllersRoot();
        Files.list(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals("GlobalExceptionHandler.java"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        assertTrue(content.contains("@PreAuthorize"),
                                p.getFileName() + " has no @PreAuthorize anywhere. " +
                                        "Every controller needs at least one for the " +
                                        "denyAllByDefault filter chain to allow any " +
                                        "request through.");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}