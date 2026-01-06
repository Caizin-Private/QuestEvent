package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthTokenController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping("")
    public Map<String, Object> authInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("message", "JWT Authentication API");
        info.put("endpoints", Map.of(
            "GET /api/auth/token", "Generate JWT token (requires authentication)",
            "GET /api/auth/me", "Get current user info (requires authentication)",
            "GET /api/auth/test", "Test JWT authentication (requires authentication)",
            "GET /api/auth/verify", "Verify which authentication method was used (JWT vs OAuth2)"
        ));
        info.put("testing", "See JWT_TESTING_GUIDE.md for complete testing instructions");
        return info;
    }

    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Generate JWT token",
            description = "Generates a JWT token for the currently authenticated user. " +
                         "Note: This endpoint requires OAuth2 session authentication first. " +
                         "Use /login to authenticate, then call this endpoint with your session cookie.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token generated successfully"),
                    @ApiResponse(responseCode = "401", description = "Not authenticated")
            }
    )
    public Map<String, Object> generateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        UserPrincipal userPrincipal = null;
        
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            userPrincipal = principal;
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            // Fallback: get user by email/username
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            userPrincipal = new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());
        } else {
            throw new RuntimeException("Unable to extract user principal");
        }
        
        String token = jwtService.generateToken(userPrincipal);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("userId", userPrincipal.userId());
        response.put("email", userPrincipal.email());
        response.put("role", userPrincipal.role());
        
        return response;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get current user info",
            description = "Returns information about the currently authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Not authenticated")
            }
    )
    public Map<String, Object> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("userId", userPrincipal.userId());
            response.put("email", userPrincipal.email());
            response.put("role", userPrincipal.role());
            response.put("authenticationType", authentication.getClass().getSimpleName());
            response.put("authenticated", authentication.isAuthenticated());
            return response;
        }
        
        throw new RuntimeException("Unable to extract user principal");
    }

    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Test JWT authentication",
            description = "Tests JWT authentication and returns authentication details",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication test successful"),
                    @ApiResponse(responseCode = "401", description = "Not authenticated")
            }
    )
    public Map<String, Object> testJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "JWT authentication is working!");
        response.put("authenticated", authentication.isAuthenticated());
        response.put("principalType", authentication.getPrincipal().getClass().getSimpleName());
        response.put("authorities", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());
        
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            response.put("userId", userPrincipal.userId());
            response.put("email", userPrincipal.email());
            response.put("role", userPrincipal.role());
        }
        
        return response;
    }

    @GetMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Verify authentication source",
            description = "Shows exactly which authentication method was used (JWT token vs OAuth2 session)",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication source verified"),
                    @ApiResponse(responseCode = "401", description = "Not authenticated")
            }
    )
    public Map<String, Object> verifyAuthSource(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check request attributes set by JwtAuthFilter
        String authSource = (String) request.getAttribute("AUTH_SOURCE");
        String authHeader = request.getHeader("Authorization");
        String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : null;
        
        Map<String, Object> response = new LinkedHashMap<>();
        
        // Authentication source detection
        if ("JWT_BEARER_TOKEN".equals(authSource)) {
            response.put("authenticationSource", "✅ JWT BEARER TOKEN");
            response.put("bearerTokenUsed", true);
            response.put("sessionCookieUsed", false);
            response.put("message", "This request was authenticated using the JWT Bearer token in the Authorization header");
            
            Long jwtUserId = (Long) request.getAttribute("JWT_USER_ID");
            if (jwtUserId != null) {
                response.put("authenticatedUserId", jwtUserId);
            }
        } else if ("OAUTH2_SESSION".equals(authSource)) {
            response.put("authenticationSource", "🔵 OAUTH2 SESSION COOKIE");
            response.put("bearerTokenUsed", false);
            response.put("sessionCookieUsed", true);
            response.put("message", "This request was authenticated using OAuth2 session cookie, NOT JWT token");
            
            Long oauthUserId = (Long) request.getAttribute("OAUTH2_USER_ID");
            if (oauthUserId != null) {
                response.put("authenticatedUserId", oauthUserId);
            }
        } else {
            response.put("authenticationSource", "⚠️ UNKNOWN");
            response.put("bearerTokenUsed", authHeader != null && authHeader.startsWith("Bearer "));
            response.put("sessionCookieUsed", sessionId != null);
            response.put("message", "Could not determine authentication source");
        }
        
        // Request details
        response.put("hasAuthorizationHeader", authHeader != null);
        response.put("authorizationHeaderPresent", authHeader != null && authHeader.startsWith("Bearer "));
        response.put("hasSessionCookie", sessionId != null);
        response.put("sessionId", sessionId);
        
        // Authentication details
        response.put("authenticationType", authentication.getClass().getSimpleName());
        response.put("authenticated", authentication.isAuthenticated());
        
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            response.put("user", Map.of(
                "userId", userPrincipal.userId(),
                "email", userPrincipal.email(),
                "role", userPrincipal.role().name()
            ));
        }
        
        response.put("authorities", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());
        
        return response;
    }
}

