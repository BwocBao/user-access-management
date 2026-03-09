package com.r2s.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        String secret = "bXlzZWNyZXRrZXlteXNlY3JldGtleW15c2VjcmV0a2V5"; // base64
        jwtUtil = new JwtUtil(secret, 15);
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

}