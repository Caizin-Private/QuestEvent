package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {

        log.debug(
                "Generating ACCESS token | userId={} | role={}",
                userPrincipal.userId(),
                userPrincipal.role()
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userPrincipal.userId());
        claims.put(CLAIM_EMAIL, userPrincipal.email());
        claims.put(CLAIM_ROLE, userPrincipal.role().name());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        String token =
                createToken(claims, userPrincipal.email(), accessExpiration);

        log.info("ACCESS token generated | userId={}", userPrincipal.userId());
        return token;
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {

        log.debug(
                "Generating REFRESH token | userId={}",
                userPrincipal.userId()
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

        String token =
                createToken(claims, userPrincipal.email(), refreshExpiration);

        log.info("REFRESH token generated | userId={}", userPrincipal.userId());
        return token;
    }

    private String createToken(
            Map<String, Object> claims,
            String subject,
            Long expirationMillis
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            boolean valid = extractExpiration(token).after(new Date());
            if (!valid) {
                log.warn("JWT validation failed: token expired");
            }
            return valid;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return TOKEN_TYPE_REFRESH.equals(
                claims.get(CLAIM_TOKEN_TYPE, String.class)
        );
    }

    public boolean isAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        return TOKEN_TYPE_ACCESS.equals(
                claims.get(CLAIM_TOKEN_TYPE, String.class)
        );
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal extractUserPrincipal(String token) {

        Claims claims = extractAllClaims(token);

        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String email = claims.get(CLAIM_EMAIL, String.class);
        String roleStr = claims.get(CLAIM_ROLE, String.class);

        if (userId == null || email == null || roleStr == null) {
            throw new IllegalStateException(
                    "Invalid ACCESS token: missing required claims"
            );
        }

        Role role = Role.valueOf(roleStr);

        log.info(
                "UserPrincipal extracted | userId={} | role={}",
                userId,
                role
        );

        return new UserPrincipal(userId, email, role);
    }
}
