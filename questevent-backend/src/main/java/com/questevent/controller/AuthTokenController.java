package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.exception.AuthTokenException;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthTokenController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthTokenController.class);

    private static final String USER_ID = "userId";
    private static final String EMAIL = "email";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping
    public Map<String, Object> authInfo() {

        log.info("Auth info endpoint accessed");

        return Map.of(
                "message", "JWT Authentication API",
                "endpoints", Map.of(
                        "GET /api/auth/token", "Generate access + refresh token (OAuth login required)",
                        "POST /api/auth/refresh", "Get new access token using refresh token",
                        "GET /api/auth/me", "Get current user info",
                        "GET /api/auth/test", "Test JWT authentication",
                        "GET /api/auth/verify", "Verify auth source (JWT vs OAuth)"
                )
        );
    }

    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate JWT access + refresh token")
    public Map<String, Object> generateTokens() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        UserPrincipal principal = extractPrincipal(authentication);

        log.info("Generating tokens for userId={} email={}",
                principal.userId(), principal.email());

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        log.debug("Tokens generated successfully for userId={}", principal.userId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 300);
        response.put(USER_ID, principal.userId());
        response.put(EMAIL, principal.email());
        response.put("role", principal.role().name());

        return response;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody Map<String, String> body) {

        log.info("Refresh token request received");

        String refreshToken = body.get("refreshToken");

        if (refreshToken == null) {
            log.warn("Refresh token missing in request body");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        if (!jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {

            log.warn("Invalid refresh token received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = jwtService.extractUsername(refreshToken);

        log.info("Refresh token validated for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found during refresh for email={}", email);
                    return new AuthTokenException("User not found during token refresh");
                });

        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

        String newAccessToken = jwtService.generateAccessToken(principal);

        log.info("New access token issued for userId={}", user.getUserId());

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer",
                "expiresIn", 300
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        UserPrincipal principal = extractPrincipal(authentication);

        log.info("Current user info requested userId={}", principal.userId());

        return Map.of(
                USER_ID, principal.userId(),
                EMAIL, principal.email(),
                "role", principal.role().name(),
                "authenticated", authentication.isAuthenticated()
        );
    }

    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> testJwt() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        log.info("JWT test endpoint hit, authenticated={}",
                authentication.isAuthenticated());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "JWT authentication working ✅");
        response.put("authenticated", authentication.isAuthenticated());
        response.put("authorities", authentication.getAuthorities());

        if (authentication.getPrincipal() instanceof UserPrincipal p) {
            response.put(USER_ID, p.userId());
            response.put(EMAIL, p.email());
            response.put("role", p.role().name());
        }

        return response;
    }

    @GetMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> verifyAuthSource(HttpServletRequest request) {

        String authSource = (String) request.getAttribute("AUTH_SOURCE");

        log.info("Auth source verification requested, source={}",
                authSource != null ? authSource : "UNKNOWN");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticationSource",
                authSource != null ? authSource : "UNKNOWN");

        response.put("hasAuthorizationHeader",
                request.getHeader("Authorization") != null);

        return response;
    }

    private UserPrincipal extractPrincipal(Authentication authentication) {

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }

        if (authentication.getPrincipal()
                instanceof org.springframework.security.core.userdetails.UserDetails ud) {

            log.debug("Extracting user from UserDetails username={}", ud.getUsername());

            User user = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found for username={}", ud.getUsername());
                        return new AuthTokenException("User not found for username");
                    });

            return new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());
        }

        log.error("Unsupported principal type: {}",
                authentication.getPrincipal().getClass().getName());

        throw new AuthTokenException("Unable to extract user principal");
    }
}
