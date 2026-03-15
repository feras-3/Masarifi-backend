package com.expensetracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for JwtUtil.
 * Covers: JWT-01 through JWT-08 (TDD §4.1)
 * Requirements: FR-AUTH-03, FR-AUTH-05
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
            "test-secret-key-for-testing-minimum-256-bits-required-here";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
    }

    // JWT-01
    @Test
    void generateToken_ReturnsNonNullToken() {
        String token = jwtUtil.generateToken("alice");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    // JWT-02
    @Test
    void generateToken_TokenContainsUsername() {
        String token = jwtUtil.generateToken("alice");
        String extracted = jwtUtil.getUsernameFromToken(token);
        assertEquals("alice", extracted);
    }

    // JWT-03
    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        String token = jwtUtil.generateToken("bob");
        assertTrue(jwtUtil.validateToken(token));
    }

    // JWT-04 — simulate expiry by setting expiration to -1 ms
    @Test
    void validateToken_WithExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L);
        String expiredToken = jwtUtil.generateToken("bob");
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    // JWT-05
    @Test
    void validateToken_WithTamperedToken_ReturnsFalse() {
        String token = jwtUtil.generateToken("charlie");
        // Corrupt the signature part (last segment)
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    // JWT-06
    @Test
    void validateToken_WithEmptyString_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    // JWT-07
    @Test
    void validateToken_WithNull_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    // JWT-08
    @Test
    void getUsernameFromToken_WithValidToken_ReturnsCorrectUsername() {
        String token = jwtUtil.generateToken("dave");
        assertEquals("dave", jwtUtil.getUsernameFromToken(token));
    }
}
