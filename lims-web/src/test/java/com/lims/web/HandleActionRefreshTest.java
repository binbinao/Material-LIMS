package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #14: the trailing {@code refresh()} after the
 * switch in {@code handleAction} fires for every case that
 * {@code break}s out of the switch (submit / start-reporting /
 * complete) — but it ALSO fires whenever a case falls through to the
 * end. While the modal cases (reject / receive-sample) currently
 * {@code return} so they skip the trailing refresh, that is fragile:
 * a future maintainer could remove the {@code return} and silently
 * regress the behavior, refreshing the page even when the user
 * clicked Cancel.
 *
 * The fix moves the {@code refresh()} call from the unconditional
 * trailing position into each non-modal case body. That makes the
 * refresh flow explicit and reviewable: every case either refreshes
 * (because it did work) or it doesn't (because the user cancelled,
 * or because the work was already done by an inner callback).
 */
class HandleActionRefreshTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    @Test
    void submitCaseRefreshesExplicitly() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        int start = content.indexOf("case 'submit':");
        int end = content.indexOf("case 'reject':", start);
        String body = content.substring(start, end);
        assertTrue(body.contains("refresh();"),
                "RequestDetail.handleAction 'submit' case must call refresh() " +
                        "explicitly after submitRequest. Today the trailing refresh " +
                        "after the switch handles it implicitly — fragile.");
    }

    @Test
    void startReportingCaseRefreshesExplicitly() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        int start = content.indexOf("case 'start-reporting':");
        int end = content.indexOf("case 'complete':", start);
        String body = content.substring(start, end);
        assertTrue(body.contains("refresh();"),
                "RequestDetail.handleAction 'start-reporting' case must " +
                        "call refresh() explicitly after startReporting.");
    }

    @Test
    void completeCaseRefreshesExplicitly() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        int start = content.indexOf("case 'complete':");
        int end = content.indexOf("default:", start);
        if (end < 0 || end - start > 400) {
            end = content.indexOf("}", start);
        }
        String body = content.substring(start, end);
        assertTrue(body.contains("refresh();"),
                "RequestDetail.handleAction 'complete' case must " +
                        "call refresh() explicitly after completeRequest.");
    }

    @Test
    void noTrailingRefreshAfterSwitch() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        int switchOpen = content.indexOf("switch (action)");
        assertTrue(switchOpen > 0, "switch (action) not found");
        int depth = 0;
        int i = switchOpen;
        for (; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) break;
            }
        }
        String after = content.substring(i, Math.min(i + 200, content.length()));
        int standaloneRefresh = -1;
        for (int j = 0; j < after.length() - 10; j++) {
            if (after.startsWith("refresh();", j) || after.startsWith("    refresh();", j)) {
                standaloneRefresh = j;
                break;
            }
        }
        assertEquals(-1, standaloneRefresh,
                "RequestDetail.handleAction must NOT have a standalone " +
                        "refresh() after the switch's closing brace. The " +
                        "non-modal cases (submit / start-reporting / complete) " +
                        "should each call refresh() in their own body. " +
                        "Today the refresh() sits at the function top level " +
                        "and fires unconditionally for any case that breaks " +
                        "out of the switch — fragile to future maintenance.");
    }
}
