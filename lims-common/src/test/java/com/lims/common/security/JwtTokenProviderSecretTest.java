package com.lims.common.security;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD test for issue #2: {@link JwtTokenProvider} ships a hard-coded default
 * value for {@code security.jwt.secret} via the {@code @Value} annotation.
 *
 * Two contracts the fix must satisfy:
 *  1. The {@code @Value} annotation must reference {@code security.jwt.secret}
 *     with NO {@code :default} fallback. If the operator forgets to set
 *     {@code JWT_SECRET}, the application must fail at startup rather than
 *     silently boot with a publicly known key.
 *  2. A {@code @PostConstruct} hook must validate that the resolved secret
 *     is at least 32 bytes (HS256 minimum), so a too-short env var also
 *     fails at startup instead of producing weak tokens.
 */
class JwtTokenProviderSecretTest {

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-common/src/main/java/com/lims/common/security/JwtTokenProvider.java");
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException("JwtTokenProvider.java not found above " + userDir);
    }

    @Test
    void jwtSecretValueAnnotationHasNoDefault() throws Exception {
        String content = readSource();

        // Match @Value("${security.jwt.secret...}") — capture whatever follows
        // the colon (if any) up to the closing brace.
        Pattern p = Pattern.compile(
                "@Value\\(\"\\$\\{security\\.jwt\\.secret(?::([^}]*))?\\}\"\\)");
        Matcher m = p.matcher(content);
        assertTrue(m.find(), "expected @Value annotation referencing security.jwt.secret");

        String defaultValue = m.group(1);
        assertNull(defaultValue,
                "security.jwt.secret @Value must not carry a :default fallback. " +
                        "Current default would let the app boot with a publicly known key " +
                        "when JWT_SECRET is missing. Found default: " + defaultValue);
    }

    @Test
    void jwtTokenProviderDeclaresPostConstructValidation() {
        boolean hasPostConstruct = false;
        for (Method m : JwtTokenProvider.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                hasPostConstruct = true;
                break;
            }
        }
        assertTrue(hasPostConstruct,
                "JwtTokenProvider must declare a @PostConstruct method to validate the " +
                        "secret at startup; without it, a too-short or unset JWT_SECRET " +
                        "would produce a running service with weak / known-key tokens.");
    }

    @Test
    void postConstructRejectsShortSecret() throws Exception {
        // Find the @PostConstruct method
        Method postConstruct = null;
        for (Method m : JwtTokenProvider.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                postConstruct = m;
                break;
            }
        }
        assertNotNull(postConstruct, "@PostConstruct method must exist (see other test)");
        postConstruct.setAccessible(true);

        // Inject a too-short secret
        JwtTokenProvider provider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(provider, "too-short");  // 9 bytes, well under 32

        try {
            postConstruct.invoke(provider);
            fail("expected IllegalStateException for secret shorter than 32 bytes");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertEquals(IllegalStateException.class, cause.getClass(),
                    "@PostConstruct must throw IllegalStateException on too-short secret, " +
                            "got: " + cause.getClass().getName() + " — " + cause.getMessage());
        }
    }
}
