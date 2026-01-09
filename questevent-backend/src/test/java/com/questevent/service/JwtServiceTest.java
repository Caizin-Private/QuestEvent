package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String SECRET =
            "my-super-secret-key-my-super-secret-key-my-super-secret-key";

    @BeforeEach
    void setup() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 5 * 60 * 1000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 60 * 60 * 1000L);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {

        UserPrincipal principal =
                new UserPrincipal(1L, "test@example.com", Role.USER);

        String token = jwtService.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {

        UserPrincipal principal =
                new UserPrincipal(2L, "test@example.com", Role.USER);

        String token = jwtService.generateRefreshToken(principal);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void shouldExtractUsernameFromToken() {

        UserPrincipal principal =
                new UserPrincipal(3L, "abc@test.com", Role.HOST);

        String token = jwtService.generateAccessToken(principal);

        String username = jwtService.extractUsername(token);

        assertEquals("abc@test.com", username);
    }

    @Test
    void shouldExtractExpirationDate() {

        UserPrincipal principal =
                new UserPrincipal(4L, "x@y.com", Role.USER);

        String token = jwtService.generateAccessToken(principal);

        Date exp = jwtService.extractExpiration(token);

        assertNotNull(exp);
        assertTrue(exp.after(new Date()));
    }

    @Test
    void shouldExtractUserPrincipalFromAccessToken() {

        Long userId = 5L;

        UserPrincipal principal =
                new UserPrincipal(userId, "user@test.com", Role.HOST);

        String token = jwtService.generateAccessToken(principal);

        UserPrincipal extracted =
                jwtService.extractUserPrincipal(token);

        assertEquals(userId, extracted.userId());
        assertEquals("user@test.com", extracted.email());
        assertEquals(Role.HOST, extracted.role());
    }

    @Test
    void shouldReturnFalseForInvalidToken() {

        String badToken = "this.is.not.valid.jwt";

        boolean valid = jwtService.validateToken(badToken);

        assertFalse(valid);
    }

    @Test
    void shouldNotTreatRefreshTokenAsAccessToken() {

        UserPrincipal principal =
                new UserPrincipal(6L, "r@test.com", Role.USER);

        String refresh = jwtService.generateRefreshToken(principal);

        assertTrue(jwtService.validateToken(refresh));
        assertTrue(jwtService.isRefreshToken(refresh));
        assertFalse(jwtService.isAccessToken(refresh));
    }
}
