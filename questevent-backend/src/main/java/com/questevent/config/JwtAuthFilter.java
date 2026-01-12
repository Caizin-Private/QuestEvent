package com.questevent.config;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

        // Skip OAuth2 internal routes
        if (
                path.startsWith("/oauth2") ||
                        path.startsWith("/login") ||
                        path.startsWith("/error") ||
                        path.startsWith("/css") ||
                        path.startsWith("/js") ||
                        path.startsWith("/images") ||
                        path.startsWith("/favicon")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApiEndpoint = path.startsWith("/api/");
        boolean isAuthInfoEndpoint = path.equals("/api/auth") || path.equals("/api/auth/");
        boolean isTokenGenerationEndpoint = path.equals("/api/auth/token");
        boolean allowOAuth2ForApi = isTokenGenerationEndpoint; // Only allow OAuth2 for token generation
        boolean isPublicApiEndpoint = isAuthInfoEndpoint; // Public API endpoints that don't require auth

        String authHeader = request.getHeader("Authorization");
        boolean hasBearerToken = authHeader != null && authHeader.startsWith("Bearer ");
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

                    request.setAttribute("AUTH_SOURCE", "JWT_BEARER_TOKEN");
                    request.setAttribute("JWT_USER_ID", userPrincipal.userId());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());

                if (isApiEndpoint && !allowOAuth2ForApi) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Unauthorized\",\"message\":\"Valid JWT Bearer token required\"}"
                    );
                    return;
                }
            }
        }
        if (isApiEndpoint && !isPublicApiEndpoint && !allowOAuth2ForApi && !hasBearerToken) {
            logger.error("❌ API endpoint requires JWT Bearer token - Path: {}, No Authorization header found", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Unauthorized\",\"message\":\"JWT Bearer token required\"}"
            );
            return;
        }

        if (isPublicApiEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication currentAuth =
                SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth instanceof UsernamePasswordAuthenticationToken token &&
                token.getPrincipal() instanceof UserPrincipal) {
            String authSource = (String) request.getAttribute("AUTH_SOURCE");
            if (authSource == null && authHeader != null && authHeader.startsWith("Bearer ")) {
                logger.debug("Authentication already set for path: {}", path);
            }

            filterChain.doFilter(request, response);
            return;
        }
        if (currentAuth instanceof OAuth2AuthenticationToken oauthToken) {

            if (isApiEndpoint && !allowOAuth2ForApi) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Use /api/auth/token to get JWT\"}"
                );
                return;
            }

            OAuth2User oauthUser = oauthToken.getPrincipal();

            String email = resolveEmail(oauthUser);

            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    logger.info(
                            "🔵 OAUTH2 SESSION AUTHENTICATION - Path: {}, User: {} (ID: {})",
                            path, user.getEmail(), user.getUserId()
                    );

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
                    request.setAttribute("AUTH_SOURCE", "OAUTH2_SESSION");
                    request.setAttribute("OAUTH2_USER_ID", user.getUserId());

                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }
            }
        }

        if (isApiEndpoint && !isPublicApiEndpoint && !allowOAuth2ForApi) {
            Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
            if (finalAuth == null || !finalAuth.isAuthenticated()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"JWT Bearer token required\"}"
                );
                return;
            }
        } else {
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
