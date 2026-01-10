package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.exception.InvalidPrincipalException;
import com.questevent.exception.UnauthenticatedUserException;
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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthTokenController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthTokenController.class);

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate JWT access + refresh token")
    public Map<String, Object> generateTokens() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedUserException("User is not authenticated");

        }

        UserPrincipal principal = extractPrincipal(authentication);

        log.info("Generating tokens for userId={} email={}",
                principal.userId(), principal.email());

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 300);
        response.put(KEY_USER_ID, principal.userId());
        response.put(KEY_EMAIL, principal.email());
        response.put(KEY_ROLE, principal.role().name());

        return response;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody Map<String, String> body) {

        log.info("Refresh token request received");

        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        if (!jwtService.validateToken(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        String email = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidPrincipalException("User not found during refresh"));

        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

        String newAccessToken = jwtService.generateAccessToken(principal);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 300);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedUserException("User is not authenticated");
        }

        UserPrincipal principal = extractPrincipal(authentication);

        return Map.of(
                KEY_USER_ID, principal.userId(),
                KEY_EMAIL, principal.email(),
                KEY_ROLE, principal.role().name(),
                "authenticated", true
        );
    }

    private UserPrincipal extractPrincipal(Authentication authentication) {

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }

        if (authentication.getPrincipal()
                instanceof org.springframework.security.core.userdetails.UserDetails ud) {

            User user = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() ->
                            new InvalidPrincipalException("User not found for username"));

            return new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());
        }

        throw new InvalidPrincipalException("Unsupported principal type");
    }
}
