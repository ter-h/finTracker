package com.fintrack.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService has no dependencies to mock, so this is a pure unit test:
 * we just construct the real object and assert on its behavior.
 *
 * Note the secret below is 32+ chars — HS256 requires a key of at least
 * 256 bits (32 bytes). A shorter secret throws WeakKeyException at construction.
 */
class JwtServiceTest {

    private static final String TEST_SECRET =
        "test-secret-key-that-is-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 60);

    @Test
    void generateToken_thenExtractUserId_returnsOriginalId() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "user@example.com");
        UUID extracted = jwtService.extractUserId(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void generateToken_producesTokenThatIsValid() {
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.com");

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        String token = jwtService.generateToken(UUID.randomUUID(), "user@example.com");
        // Flip the last character of the signature — token should no longer verify
        String tampered = token.substring(0, token.length() - 1)
            + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_withGarbageString_returnsFalse() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() {
        // expiryMinutes = -1 -> expiry timestamp is already in the past the moment it's issued
        JwtService expiringImmediately = new JwtService(TEST_SECRET, -1);
        String token = expiringImmediately.generateToken(UUID.randomUUID(), "user@example.com");

        assertThat(expiringImmediately.isValid(token)).isFalse();
    }

    @Test
    void extractUserId_withInvalidToken_throws() {
        assertThatThrownBy(() -> jwtService.extractUserId("garbage"))
            .isInstanceOf(JwtException.class);
    }
}
