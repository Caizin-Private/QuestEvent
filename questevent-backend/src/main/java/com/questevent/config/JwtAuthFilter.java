package com.questevent.config;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public JwtAuthFilter(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Skip OAuth2 and login paths entirely (they don't need JWT processing)
        if (path.startsWith("/oauth2") || path.startsWith("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // For API endpoints (except /api/auth info endpoint and /api/auth/token which needs OAuth2 session),
        // enforce JWT token authentication and reject OAuth2 sessions
        boolean isApiEndpoint = path.startsWith("/api/");
        boolean isAuthInfoEndpoint = path.equals("/api/auth") || path.equals("/api/auth/");
        boolean isTokenGenerationEndpoint = path.equals("/api/auth/token");
        boolean allowOAuth2ForApi = isTokenGenerationEndpoint; // Only allow OAuth2 for token generation
        boolean isPublicApiEndpoint = isAuthInfoEndpoint; // Public API endpoints that don't require auth
        
        String authHeader = request.getHeader("Authorization");
        boolean hasBearerToken = authHeader != null && authHeader.startsWith("Bearer ");
        
        // First, try to extract JWT token from Authorization header
        if (hasBearerToken) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.validateToken(token)) {
                    UserPrincipal userPrincipal = jwtService.extractUserPrincipal(token);
                    
                    logger.info("✅ JWT TOKEN AUTHENTICATION - Path: {}, User: {} (ID: {}), Token used: YES", 
                        path, userPrincipal.email(), userPrincipal.userId());
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + userPrincipal.role().name()
                                    ))
                            );
                    
                    // Set a request attribute to track that JWT was used
                    request.setAttribute("AUTH_SOURCE", "JWT_BEARER_TOKEN");
                    request.setAttribute("JWT_USER_ID", userPrincipal.userId());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());
                
                // For API endpoints (except token generation), reject invalid tokens
                if (isApiEndpoint && !allowOAuth2ForApi) {
                    logger.error("❌ API endpoint requires valid JWT token - Path: {}", path);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Valid JWT Bearer token required for API endpoints\"}");
                    return;
                }
            }
        }
        
        // For API endpoints (except public endpoints and /api/auth/token), require JWT token - reject if not present
        if (isApiEndpoint && !isPublicApiEndpoint && !allowOAuth2ForApi && !hasBearerToken) {
            logger.error("❌ API endpoint requires JWT Bearer token - Path: {}, No Authorization header found", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT Bearer token required in Authorization header for API endpoints\"}");
            return;
        }
        
        // Allow public API endpoints to proceed without authentication
        if (isPublicApiEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication currentAuth =
                SecurityContextHolder.getContext().getAuthentication();

        // ✅ Already converted → check if it was from JWT or OAuth2
        if (currentAuth instanceof UsernamePasswordAuthenticationToken token &&
                token.getPrincipal() instanceof UserPrincipal) {
            
            // If we have a Bearer token but authentication was already set, 
            // it might be from a previous filter - check request attributes
            String authSource = (String) request.getAttribute("AUTH_SOURCE");
            if (authSource == null && authHeader != null && authHeader.startsWith("Bearer ")) {
                // Authentication exists but wasn't set by our JWT filter - likely from cache
                logger.debug("Authentication already set for path: {}", path);
            }
            
            filterChain.doFilter(request, response);
            return;
        }
        
        // Handle OAuth2 authentication tokens
        // Only allow OAuth2 session for non-API endpoints or /api/auth/token (to get initial token)
        if (currentAuth instanceof OAuth2AuthenticationToken oauthToken) {
            
            // Reject OAuth2 session for API endpoints (except token generation)
            if (isApiEndpoint && !allowOAuth2ForApi) {
                logger.error("❌ API endpoint rejected OAuth2 session - Path: {}, JWT Bearer token required", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"API endpoints require JWT Bearer token. Please use /api/auth/token to get a token first.\"}");
                return;
            }

            OAuth2User oauthUser = oauthToken.getPrincipal();

            String email = resolveEmail(oauthUser);

            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    logger.info("🔵 OAUTH2 SESSION AUTHENTICATION - Path: {}, User: {} (ID: {}), Token used: NO", 
                        path, user.getEmail(), user.getUserId());

                    UserPrincipal principal = new UserPrincipal(
                            user.getUserId(),
                            user.getEmail(),
                            user.getRole()
                    );

                    UsernamePasswordAuthenticationToken newAuth =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + user.getRole().name()
                                    ))
                            );
                    
                    // Set a request attribute to track that OAuth2 session was used
                    request.setAttribute("AUTH_SOURCE", "OAUTH2_SESSION");
                    request.setAttribute("OAUTH2_USER_ID", user.getUserId());

                    SecurityContextHolder.getContext()
                            .setAuthentication(newAuth);
                }
            }
        }
        
        // For API endpoints (except public ones), reject if no authentication was found
        if (isApiEndpoint && !isPublicApiEndpoint && !allowOAuth2ForApi) {
            Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
            if (finalAuth == null || !finalAuth.isAuthenticated()) {
                logger.error("❌ API endpoint - No authentication found - Path: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"JWT Bearer token required for API endpoints\"}");
                return;
            }
        } else {
            // Log if no authentication was found (for non-API endpoints)
            if (SecurityContextHolder.getContext().getAuthentication() == null || 
                !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                logger.warn("⚠️ NO AUTHENTICATION - Path: {}, Authorization header present: {}", 
                    path, authHeader != null);
            }
        }

        filterChain.doFilter(request, response);
    }
    private String resolveEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
