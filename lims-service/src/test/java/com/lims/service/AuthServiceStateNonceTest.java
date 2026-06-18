package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #4: OAuth state / nonce are missing from the
 * Azure AD flow, and the callback endpoint is GET (so the authorization
 * code leaks through referer / proxy logs).
 *
 * Four contracts the fix must satisfy:
 *  1. {@code AuthService.getAuthorizationUrl} must include a {@code state}
 *     parameter and a {@code nonce} parameter, and must persist both for
 *     verification on callback.
 *  2. {@code AuthService.handleCallback} must validate the incoming
 *     {@code state} against the persisted value (CSRF protection).
 *  3. {@code AuthService.parseIdTokenClaims} / handleCallback must validate
 *     the id_token's {@code nonce} claim against the persisted value
 *     (replay protection).
 *  4. {@code AuthController.callback} must be a {@code @PostMapping}, not
 *     {@code @GetMapping}, so the authorization code is sent in the
 *     request body and never ends up in URLs / referer headers / proxy
 *     access logs.
 */
class AuthServiceStateNonceTest {

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
    void getAuthorizationUrlIncludesStateAndNonceParameters() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/AuthService.java");
        // Look for the URL parameter "state=" / "nonce=" (case-insensitive).
        // Allow either a query-string separator (? or &) before the parameter
        // name, and don't require quotes around it (the URL builder emits raw
        // key=value, not Java string literals).
        boolean hasStateParam = Pattern.compile("(?:[?&])state=", Pattern.CASE_INSENSITIVE)
                .matcher(content).find();
        boolean hasNonceParam = Pattern.compile("(?:[?&])nonce=", Pattern.CASE_INSENSITIVE)
                .matcher(content).find();
        assertTrue(hasStateParam,
                "AuthService.getAuthorizationUrl must include a 'state=' URL parameter " +
                        "(OAuth 2.0 CSRF protection). Today it only sets client_id, " +
                        "response_type, redirect_uri, scope, and response_mode.");
        assertTrue(hasNonceParam,
                "AuthService.getAuthorizationUrl must include a 'nonce=' URL parameter " +
                        "(OpenID Connect replay protection). Today the id_token's nonce " +
                        "claim is never set by the relying party, so it cannot be " +
                        "validated on callback.");
    }

    @Test
    void handleCallbackValidatesStateAgainstSession() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/AuthService.java");
        // The fix must:
        //   - take a state parameter (either on handleCallback signature or in some
        //     session-shaped storage)
        //   - compare it against the persisted state and reject on mismatch.
        // We check for a session-store lookup + a state-equality branch.
        boolean hasSessionGet = content.contains("getAttribute(") || content.contains(".get(\"state\")");
        boolean hasStateComparison = Pattern.compile(
                "(expectedState|state\\.equals|stateFromSession|expected.*state)",
                Pattern.CASE_INSENSITIVE).matcher(content).find();
        assertTrue(hasSessionGet && hasStateComparison,
                "AuthService.handleCallback must look up the expected state from " +
                        "session storage and reject the request if the incoming state " +
                        "does not match. Today the method only accepts a 'code' parameter; " +
                        "a CSRF attacker can submit a stolen code with any state value " +
                        "(or none) and the server will accept it.");
    }

    @Test
    void handleCallbackValidatesNonceClaimAgainstSession() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/AuthService.java");
        // The fix must compare the id_token's nonce claim against the
        // session-stored nonce and reject on mismatch.
        boolean hasNonceClaim = Pattern.compile(
                "(\"nonce\"|claims\\.get\\(\"nonce\"|claims\\.getClaim\\(\"nonce\")",
                Pattern.CASE_INSENSITIVE).matcher(content).find();
        boolean hasNonceComparison = Pattern.compile(
                "nonce.*equals|expectedNonce|nonceFromSession",
                Pattern.CASE_INSENSITIVE).matcher(content).find();
        assertTrue(hasNonceClaim && hasNonceComparison,
                "AuthService must read the id_token's 'nonce' claim and compare it " +
                        "against the session-stored nonce. Without this check, a replayed " +
                        "id_token from a prior session (still within Azure AD's ~1h " +
                        "expiry) could be accepted for login.");
    }

    @Test
    void authControllerCallbackIsPostMappingNotGetMapping() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/AuthController.java");
        // Find the @GetMapping/@PostMapping directly above the callback() method.
        // We expect @PostMapping, not @GetMapping.
        int callbackIdx = content.indexOf("\"/callback\"");
        assertNotEquals(-1, callbackIdx, "AuthController.callback endpoint not found");
        // Look at the ~200 chars before the @...Mapping annotation. The mapping
        // itself is captured by a wider window so the regex can see the
        // annotation and the literal path together.
        int windowStart = Math.max(0, callbackIdx - 200);
        String window = content.substring(windowStart, callbackIdx + "\"/callback\"".length());
        // The Spring annotations look like @GetMapping("/callback") or
        // @PostMapping("/callback"); we allow either \" or ( between the
        // annotation keyword and the path, with optional whitespace.
        boolean hasGetOnCallback = Pattern.compile("@GetMapping\\s*[\"(]")
                .matcher(window).find();
        boolean hasPostOnCallback = Pattern.compile("@PostMapping\\s*[\"(]")
                .matcher(window).find();
        assertTrue(hasPostOnCallback && !hasGetOnCallback,
                "AuthController.callback must be a @PostMapping (not @GetMapping) so " +
                        "the OAuth authorization code travels in the request body, not the " +
                        "URL. Today it is a GET, so the code is captured in browser " +
                        "history, HTTP referer headers, and reverse-proxy access logs.");
    }
}
