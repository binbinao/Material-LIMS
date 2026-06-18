package com.lims.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #1: dev profile 全员 ADMIN.
 *
 * Two contracts that the fix must satisfy:
 *  1. {@link DevAuthFilter} is annotated {@code @Profile("dev")} so it cannot
 *     be wired into the prod application context even if a future refactor
 *     promotes it from inline instantiation to a Spring bean.
 *  2. {@code docker-compose.yml} does not silently boot the prod-shaped
 *     {@code lims-backend} service with {@code SPRING_PROFILES_ACTIVE=dev},
 *     which would activate the {@code devFilterChain} permit-all chain and
 *     inject a synthetic ADMIN principal for every request.
 */
class DevAuthFilterProfileTest {

    @Test
    void devAuthFilterClassCarriesDevProfileAnnotation() throws Exception {
        Profile profile = DevAuthFilter.class.getAnnotation(Profile.class);

        assertNotNull(profile,
                "DevAuthFilter must be annotated @Profile(\"dev\") so it cannot be " +
                        "registered as a Spring bean in the prod profile");

        boolean matchesDev = Arrays.asList(profile.value()).contains("dev");
        assertTrue(matchesDev,
                "@Profile on DevAuthFilter must include \"dev\"; actual values: " +
                        Arrays.toString(profile.value()));
    }

    @Test
    void dockerComposeDoesNotEnableDevProfileForBackend() throws Exception {
        // Walk up from the test working dir until we find docker-compose.yml
        // (works for both `mvn test` in lims-web/ and direct IDE launches).
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path compose = null;
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("docker-compose.yml");
            if (Files.isRegularFile(candidate)) {
                compose = candidate;
                break;
            }
        }
        assertNotNull(compose, "docker-compose.yml not found above " + userDir);

        String content = Files.readString(compose);
        assertFalse(content.contains("SPRING_PROFILES_ACTIVE: dev"),
                "docker-compose.yml must not pin SPRING_PROFILES_ACTIVE=dev for the " +
                        "lims-backend service; doing so activates the dev permit-all chain " +
                        "and DevAuthFilter's synthetic ADMIN principal in any deployment " +
                        "that uses this compose file unchanged");
    }
}
