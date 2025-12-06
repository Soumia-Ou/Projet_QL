package com.example.pfa.reservation;


import com.example.pfa.reservation.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "0123456789ABCDEFGHIJKLMNOPQRSTUVWX"; // 32+ chars

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret);
    }

    @Test
    void generateAndExtractUsername_andValidateToken_success() {
        String username = "user1";
        String role = "CLIENT";

        String token = jwtUtil.generateToken(username, role);
        assertNotNull(token);

        String extracted = jwtUtil.extractUsername(token);
        assertEquals(username, extracted);

        UserDetails ud = User.withUsername(username).password("pwd").authorities(Collections.emptyList()).build();
        assertTrue(jwtUtil.validateToken(token, ud));
    }

    @Test
    void validateToken_wrongUsername_returnsFalse() {
        String token = jwtUtil.generateToken("userA", "CLIENT");
        UserDetails ud = User.withUsername("otherUser").password("pwd").authorities(Collections.emptyList()).build();

        assertFalse(jwtUtil.validateToken(token, ud));
    }

    @Test
    void extractAllClaims_invalidToken_throwsRuntimeException() {
        String invalid = "this.is.not.a.token";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jwtUtil.extractAllClaims(invalid));
        assertTrue(ex.getMessage().toLowerCase().contains("impossible d'extraire"));
    }

    @Test
    void constructor_shortSecret_throwsIllegalArgumentException() {
        String shortSecret = "short";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new JwtUtil(shortSecret));
        assertTrue(e.getMessage().contains("32"));
    }
}

