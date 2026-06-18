package com.lims.service.storage;

import org.junit.jupiter.api.Test;

public class FileStorageServiceUnicodeTest {

    @Test
    void sanitizeFilenameStripsBidiControls() {
        // Use unicode escape so heredoc/bash doesn't pass a literal
        // bidi override or null byte that the snapshot tooling rejects.
        String input = "evil\u202Efile\u0000.txt";
        String out = FileStorageService.sanitizeFilename(input);
        if (out.contains("\u202E")) {
            throw new AssertionError("sanitize must drop U+202E bidi override; got: " + out);
        }
        if (out.contains("\u0000")) {
            throw new AssertionError("sanitize must drop null bytes; got: " + out);
        }
    }
}
