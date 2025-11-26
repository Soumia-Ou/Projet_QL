package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        // une clé de 32+ caractères
        jwtUtil = new JwtUtil("01234567890123456789012345678901");
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("karima", "HOTEL_ADMIN");

        UserDetails userDetails = new User(
                "karima",
                "password",
                Collections.emptyList()
        );

        Assertions.assertEquals("karima", jwtUtil.extractUsername(token));
        Assertions.assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void testExtractRole() {
        String token = jwtUtil.generateToken("karima", "CLIENT");

        String role = jwtUtil.extractAllClaims(token).get("role", String.class);

        Assertions.assertEquals("CLIENT", role);
    }

    @Test
    void testInvalidTokenThrowsException() {
        Assertions.assertThrows(Exception.class, () -> {
            jwtUtil.extractAllClaims("invalid.token.here");
        });
    }
}
