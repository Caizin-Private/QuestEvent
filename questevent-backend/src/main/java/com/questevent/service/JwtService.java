package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

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
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.userId());
        claims.put("email", userPrincipal.email());
        claims.put("role", userPrincipal.role().name());
        claims.put("tokenType", "ACCESS");

        return createToken(claims, userPrincipal.email(), accessExpiration);
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");

        return createToken(claims, userPrincipal.email(), refreshExpiration);
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
            return extractExpiration(token).after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return "REFRESH".equals(claims.get("tokenType", String.class));
    }

    public boolean isAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        return "ACCESS".equals(claims.get("tokenType", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
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

        UUID userId = claims.get("userId", UUID.class);
        String email = claims.get("email", String.class);
        String roleStr = claims.get("role", String.class);

        if (userId == null || email == null || roleStr == null) {
            throw new RuntimeException("Invalid ACCESS token: missing required claims");
        }

        Role role = Role.valueOf(roleStr);

        return new UserPrincipal(userId, email, role);
    }
}
