package com.r2s.core.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        String secret = "bXlzZWNyZXRrZXlteXNlY3JldGtleW15c2VjcmV0a2V5"; // base64
        jwtUtil = new JwtUtil(secret, 15,5);
    }

    @Test
    void generateToken_and_extractUsername() {

        String token = jwtUtil.generateToken("beo9", "ROLE_USER");

        String username = jwtUtil.extractUsername(token);

        assertEquals("beo9", username);
    }

    @Test
    void extractRole_shouldReturnRole() {

        String token = jwtUtil.generateToken("beo9", "ROLE_ADMIN");

        String role = jwtUtil.extractRoles(token);

        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void generateServiceToken_shouldContainServiceType() {

        String token = jwtUtil.generateServiceToken("auth-service");

        String username = jwtUtil.extractUsername(token);

        assertEquals("auth-service", username);
    }

    @Test
    void token_shouldContainExpiration() {

        String token = jwtUtil.generateToken("beo9","ROLE_USER");

        Claims claims = jwtUtil.extractAllClaims(token);

        assertNotNull(claims.getExpiration());
    }

    @Test
    void extractClaims_shouldThrowException_whenTokenInvalid() {

        String invalidToken = "abc.def.ghi";

        assertThrows(Exception.class, () -> {
            jwtUtil.extractAllClaims(invalidToken);
        });
    }

}