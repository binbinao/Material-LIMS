package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JestConfigAndCiTest {

    @Test
    void githubWorkflowRunsNpmTest() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path workflowsDir = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(".github/workflows");
            if (Files.isDirectory(candidate)) {
                workflowsDir = candidate;
                break;
            }
        }
        assertTrue(workflowsDir != null, ".github/workflows/ must exist");
        boolean found = false;
        try (var stream = Files.list(workflowsDir)) {
            for (Path wf : StreamSupport.stream(stream.spliterator(), false)
                    .collect(Collectors.toList())) {
                String name = wf.toString();
                if (!name.endsWith(".yml") && !name.endsWith(".yaml")) continue;
                if (Files.readString(wf).contains("npm test")) { found = true; break; }
            }
        }
        assertTrue(found, "At least one .github/workflows/*.yml must run npm test");
    }

    @Test
    void githubWorkflowRunsMvnTest() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path workflowsDir = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(".github/workflows");
            if (Files.isDirectory(candidate)) {
                workflowsDir = candidate;
                break;
            }
        }
        assertTrue(workflowsDir != null, ".github/workflows/ must exist");
        boolean found = false;
        try (var stream = Files.list(workflowsDir)) {
            for (Path wf : StreamSupport.stream(stream.spliterator(), false)
                    .collect(Collectors.toList())) {
                String name = wf.toString();
                if (!name.endsWith(".yml") && !name.endsWith(".yaml")) continue;
                String content = Files.readString(wf);
                if (content.contains("./mvnw") && content.contains("test")) {
                    found = true; break;
                }
            }
        }
        assertTrue(found, "At least one .github/workflows/*.yml must run mvnw test");
    }
}
