package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
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

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping
    public Map<String, Object> authInfo() {
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
    @Operation(
            summary = "Generate JWT access + refresh token",
            description = "Requires OAuth2 login first"
    )
    public Map<String, Object> generateTokens() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        UserPrincipal principal = extractPrincipal(authentication);

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 300); // 5 minutes
        response.put("userId", principal.userId());
        response.put("email", principal.email());
        response.put("role", principal.role().name());

        return response;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Generate new access token using refresh token"
    )
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || !jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

        String newAccessToken = jwtService.generateAccessToken(principal);

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

        return Map.of(
                "userId", principal.userId(),
                "email", principal.email(),
                "role", principal.role().name(),
                "authenticated", authentication.isAuthenticated()
        );
    }

    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> testJwt() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "JWT authentication working ✅");
        response.put("authenticated", authentication.isAuthenticated());
        response.put("authorities", authentication.getAuthorities());

        if (authentication.getPrincipal() instanceof UserPrincipal p) {
            response.put("userId", p.userId());
            response.put("email", p.email());
            response.put("role", p.role().name());
        }

        return response;
    }

    @GetMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> verifyAuthSource(HttpServletRequest request) {

        String authSource = (String) request.getAttribute("AUTH_SOURCE");

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

        if (authentication.getPrincipal() instanceof
                org.springframework.security.core.userdetails.UserDetails ud) {

            User user = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());
        }

        throw new RuntimeException("Unable to extract user principal");
    }
}
