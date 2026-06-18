package com.lims.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #3: {@link AuthService#parseIdTokenClaims} (and the
 * surrounding id_token flow) does not verify the JWT signature before
 * trusting its claims. Anyone with network access to the OAuth callback
 * endpoint can submit a self-signed id_token and have the server issue
 * a LIMS JWT for whatever email they choose.
 *
 * The four contracts the fix must satisfy, asserted at the source level
 * (a heavier behavioural test would need a mocked JWKS provider and a
 * live RestTemplate stub — that scaffolding will be added in a follow-up
 * once the structural shape is in place):
 *
 *  1. The file imports at least one JWKS / JWS class from nimbus-jose-jwt
 *     (e.g. {@code JWKSet}, {@code JWSVerifier}, {@code JWSObject},
 *     {@code RSAKey}) so signature verification is structurally present.
 *  2. There is an explicit {@code iss} (issuer) check that the id_token's
 *     issuer claim matches {@code https://login.microsoftonline.com/{tenantId}/v2.0}
 *     or an equivalent pattern; without it, an attacker can replay tokens
 *     from any other Azure tenant.
 *  3. There is an explicit {@code aud} (audience) check against the
 *     configured {@code clientId}; without it, an id_token minted for a
 *     different application in the same tenant would be accepted.
 *  4. There is an explicit {@code exp} (expiration) check on the id_token
 *     itself; without it, an expired token could be replayed.
 */
class AuthServiceIdTokenVerificationTest {

    private static String readSource() throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "lims-service/src/main/java/com/lims/service/AuthService.java");
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException("AuthService.java not found above " + userDir);
    }

    @Test
    void authServiceImportsJwksClassForSignatureVerification() throws Exception {
        String content = readSource();
        // Match any nimbus JOSE/JWT class import — `com.nimbusds.jose.*` (jwk, jws, jwe, jose, crypto)
        // and `com.nimbusds.jwt.*` (SignedJWT, JWTClaimsSet, JWKSet). We require at least
        // one of {JWKSet, JWSVerifier, JWSObject, RSAKey} to be imported, since those are
        // the ones actually used for signature verification.
        Pattern p = Pattern.compile(
                "import\\s+com\\.nimbusds\\.(jose|jwt)\\.[^;]*\\.(JWKSet|JWSVerifier|JWSObject|RSAKey)\\b");
        Matcher m = p.matcher(content);
        assertTrue(m.find(),
                "AuthService must import at least one JWKS / JWS class from " +
                        "com.nimbusds.jose.* or com.nimbusds.jwt.* (JWKSet / JWSVerifier / " +
                        "JWSObject / RSAKey) so id_token signature verification is " +
                        "structurally possible. Today it only imports cn.hutool.jwt.JWTUtil " +
                        "which has no JWKS support.");
    }

    @Test
    void authServiceValidatesIdTokenIssuerClaim() throws Exception {
        String content = readSource();
        // Look for an iss check that references microsoftonline.com — the Azure AD
        // v2.0 issuer format. We don't enforce a particular expression form, just
        // that the comparison is there. The fix uses nimbus's typed JWTClaimsSet
        // API (claims.getIssuer()) rather than the loose map.get("iss") pattern.
        boolean hasMicrosoftonlineDomain = content.contains("login.microsoftonline.com");
        boolean hasIssCheck = content.contains("getIssuer(")
                || content.contains("claims.get(\"iss\")");
        assertTrue(hasMicrosoftonlineDomain && hasIssCheck,
                "AuthService must validate the id_token's iss claim against " +
                        "https://login.microsoftonline.com/{tenantId}/v2.0 (or a substring " +
                        "thereof). Today parseIdTokenClaims accepts any id_token without " +
                        "checking the issuer, so a token from a different Azure tenant " +
                        "would be accepted.");
    }

    @Test
    void authServiceValidatesIdTokenAudienceClaim() throws Exception {
        String content = readSource();
        boolean hasAudCheck = content.contains("getAudience(")
                || content.contains("claims.get(\"aud\")");
        assertTrue(hasAudCheck,
                "AuthService must validate the id_token's aud claim against the " +
                        "configured azure.ad.client-id. Today parseIdTokenClaims does not " +
                        "check audience, so an id_token minted for a different application " +
                        "in the same tenant would be accepted.");
    }

    @Test
    void authServiceValidatesIdTokenExpirationClaim() throws Exception {
        String content = readSource();
        boolean hasExpCheck = content.contains("getExpirationTime(")
                || content.contains("claims.get(\"exp\")");
        assertTrue(hasExpCheck,
                "AuthService must validate the id_token's exp claim at parse time. " +
                        "Today parseIdTokenClaims only extracts sub/email/oid/name — an " +
                        "expired Azure AD id_token would be accepted as long as its " +
                        "signature (currently unverified) is well-formed.");
    }
}
