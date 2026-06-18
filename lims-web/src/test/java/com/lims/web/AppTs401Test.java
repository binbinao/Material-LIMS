package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #13: {@code lims-web-ui/src/app.tsx} silently
 * swallows 401/403 from {@code /api/v1/auth/me} and from any request.
 *
 * Two contracts the fix must satisfy (asserted at source level):
 *
 *  1. {@code getInitialState} explicitly handles a 401 from
 *     {@code /api/v1/auth/me} — it must check {@code res.status} (or
 *     {@code res.ok}) and short-circuit the JWT-subject path so the
 *     app boots into a logged-out state instead of treating an expired
 *     token as a valid user with a malformed payload.
 *  2. {@code errorHandler} redirects to the login route (not just logs
 *     a {@code console.warn}) when any subsequent request returns 401,
 *     so the user can re-authenticate instead of being stuck on a
 *     page that silently fails every API call.
 */
class AppTs401Test {

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
    void getInitialStateHandles401Explicitly() throws Exception {
        String content = readSource("lims-web-ui/src/app.tsx");
        boolean checksStatus = content.contains("res.status")
                || content.contains("res.ok")
                || content.contains("response.status")
                || content.contains("!res.ok");
        assertTrue(checksStatus,
                "app.tsx getInitialState must check the fetch response status " +
                        "before calling res.json(). Today the function blindly " +
                        "parses the body and silently returns {} on 401.");
    }

    @Test
    void errorHandlerRedirectsToLoginOn401() throws Exception {
        String content = readSource("lims-web-ui/src/app.tsx");
        boolean hasRedirect = content.contains("history.replace")
                || content.contains("window.location.href")
                || content.contains("history.push('/login'")
                || content.contains("history.push(\"/login\"");
        assertTrue(hasRedirect,
                "app.tsx errorHandler must redirect to /login on 401, not just " +
                        "log a console.warn. Today the user stays on the current " +
                        "page and every API call fails silently.");
    }
}
