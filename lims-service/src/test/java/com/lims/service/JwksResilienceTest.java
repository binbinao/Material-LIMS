package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #17 (post-fix review H-1): {@code AuthService.loadJwks}
 * hits Microsoft's JWKS endpoint synchronously on every login callback
 * with no circuit-breaker, no fallback, and no friendly error. When the
 * JWKS endpoint is slow or unreachable, every Azure AD login times out
 * with HTTP 500 instead of a clean "login provider unavailable" signal.
 *
 * Three contracts the fix must satisfy (asserted at source level — a real
 * test would need to mock the RestTemplate and inject a slow JWKS server):
 *
 *  1. {@code AuthService.loadJwks} is wrapped in either a Resilience4j
 *     {@code @CircuitBreaker} / {@code @TimeLimiter} annotation OR has an
 *     explicit try/catch that turns JWKS failures into a domain exception
 *     the caller can distinguish from "bad token".
 *  2. {@code ErrorCode} has a {@code LOGIN_PROVIDER_UNAVAILABLE} entry so
 *     the frontend can show "SSO temporarily unavailable" instead of
 *     "Internal server error".
 *  3. {@code AuthService} has a startup hook (e.g. {@code @PostConstruct}
 *     or {@code @EventListener(ApplicationReadyEvent.class)}) that
 *     pre-warms the JWKS cache so the first user login doesn't pay the
 *     JWKS fetch latency.
 */
class JwksResilienceTest {

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
    void loadJwksIsWrappedInCircuitBreakerOrTimeLimiter() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/AuthService.java");
        boolean hasResilienceAnnotation = content.contains("@CircuitBreaker")
                || content.contains("@TimeLimiter")
                || content.contains("@Retry")
                || content.contains("@Bulkhead");
        int loadJwksIdx = content.indexOf("loadJwks(");
        assertNotEquals(-1, loadJwksIdx, "loadJwks method not found");
        int bodyStart = content.indexOf('{', loadJwksIdx);
        int bodyEnd = -1;
        int depth = 0;
        for (int i = bodyStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { bodyEnd = i; break; }
            }
        }
        String body = content.substring(bodyStart, bodyEnd);
        boolean hasTryCatch = body.contains("try")
                && body.contains("catch")
                && (body.contains("BusinessException") || body.contains("throw "));
        assertTrue(hasResilienceAnnotation || hasTryCatch,
                "AuthService.loadJwks must be wrapped in either a Resilience4j " +
                        "annotation (@CircuitBreaker / @TimeLimiter / @Retry / " +
                        "@Bulkhead) or an explicit try/catch that translates " +
                        "Microsoft JWKS network failures into a domain exception. " +
                        "Today a 5 s timeout on every login = denial of login when " +
                        "the JWKS endpoint is slow.");
    }

    @Test
    void errorCodeHasLoginProviderUnavailable() throws Exception {
        String content = readSource(
                "lims-common/src/main/java/com/lims/common/exception/ErrorCode.java");
        assertTrue(content.contains("LOGIN_PROVIDER_UNAVAILABLE")
                        || content.contains("SSO_UNAVAILABLE")
                        || content.contains("OAUTH_PROVIDER_UNAVAILABLE"),
                "ErrorCode enum must declare a LOGIN_PROVIDER_UNAVAILABLE (or " +
                        "similarly named) error code so the frontend can show a " +
                        "clean 'SSO temporarily unavailable' message instead of a " +
                        "generic 'Internal server error'.");
    }

    @Test
    void authServiceHasStartupHookToPrewarmJwks() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/AuthService.java");
        boolean hasPostConstruct = content.contains("@PostConstruct")
                && content.contains("loadJwks");
        boolean hasEventListener = content.contains("@EventListener")
                && content.contains("ApplicationReadyEvent")
                && content.contains("loadJwks");
        boolean hasCommandLineRunner = content.contains("CommandLineRunner")
                && content.contains("loadJwks");
        boolean hasApplicationReady = content.contains("ApplicationReadyEvent")
                && content.contains("loadJwks");
        assertTrue(hasPostConstruct || hasEventListener || hasCommandLineRunner
                        || hasApplicationReady,
                "AuthService must pre-warm the JWKS cache at startup so the " +
                        "first user login doesn't pay the JWKS fetch latency.");
    }
}