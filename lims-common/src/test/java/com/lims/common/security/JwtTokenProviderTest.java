package com.lims.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-32-bytes-min-len-required-1234";

    @Test
    @DisplayName("generated token round-trips userId, email, roles, deptId")
    void roundTrip() {
        JwtTokenProvider p = newProvider();
        String token = p.generate("u-1", "alice@example.com", "Alice", "ADMIN,ENGINEER", "d-9");
        JwtTokenProvider.AuthPrincipal principal = p.parse(token);

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo("u-1");
        assertThat(principal.email()).isEqualTo("alice@example.com");
        assertThat(principal.displayName()).isEqualTo("Alice");
        assertThat(principal.roles()).isEqualTo("ADMIN,ENGINEER");
        assertThat(principal.deptId()).isEqualTo("d-9");
    }

    @Test
    @DisplayName("garbage token returns null instead of throwing")
    void garbageTokenReturnsNull() {
        JwtTokenProvider p = newProvider();
        assertThat(p.parse("not-a-jwt")).isNull();
        assertThat(p.parse("")).isNull();
        assertThat(p.parse(null)).isNull();
    }

    @Test
    @DisplayName("hasRole matches case-insensitively across the comma list")
    void hasRoleIsCaseInsensitive() {
        JwtTokenProvider p = newProvider();
        String token = p.generate("u-2", "b@x", "Bob", "manager,ENGINEER", null);
        JwtTokenProvider.AuthPrincipal principal = p.parse(token);

        assertThat(principal).isNotNull();
        assertThat(principal.hasRole("MANAGER")).isTrue();
        assertThat(principal.hasRole("engineer")).isTrue();
        assertThat(principal.hasRole("ADMIN")).isFalse();
    }

    private static JwtTokenProvider newProvider() {
        JwtTokenProvider p = new JwtTokenProvider();
        ReflectionTestUtils.setField(p, "secret", SECRET);
        ReflectionTestUtils.setField(p, "ttlHours", 8L);
        return p;
    }
}
