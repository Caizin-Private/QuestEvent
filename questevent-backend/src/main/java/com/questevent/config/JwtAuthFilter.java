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

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String APPLICATION_JSON = "application/json";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final String AUTH_SOURCE = "AUTH_SOURCE";

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

        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApiEndpoint = path.startsWith("/api/");
        boolean isAuthInfoEndpoint = isAuthInfoEndpoint(path);
        boolean allowOAuth2ForApi = isTokenEndpoint(path);

        String authHeader = request.getHeader("Authorization");

        if (tryJwtAuthentication(
                request,
                response,
                filterChain,
                path,
                authHeader,
                isApiEndpoint,
                allowOAuth2ForApi
        )) {
            return;
        }

        if (isApiEndpoint && !isAuthInfoEndpoint && !allowOAuth2ForApi && authHeader == null) {
            sendJwtRequired(response);
            return;
        }

        if (isAuthInfoEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        if (handleExistingAuthentication(
                request,
                response,
                path,
                isApiEndpoint,
                allowOAuth2ForApi
        )) {
            return;
        }

        enforceFinalAuthentication(
                response,
                path,
                authHeader,
                isApiEndpoint
        );

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/oauth2")
                || path.startsWith("/login")
                || path.startsWith("/error")
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images")
                || path.startsWith("/favicon");
    }

    private boolean isAuthInfoEndpoint(String path) {
        return "/api/auth".equals(path) || "/api/auth/".equals(path);
    }

    private boolean isTokenEndpoint(String path) {
        return "/api/auth/token".equals(path);
    }

    private boolean tryJwtAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String path,
            String authHeader,
            boolean isApiEndpoint,
            boolean allowOAuth2ForApi
    ) throws IOException, ServletException {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        try {
            String token = authHeader.substring(7);

            if (!jwtService.validateToken(token)) {
                return false;
            }

            UserPrincipal principal = jwtService.extractUserPrincipal(token);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "✅ JWT TOKEN AUTHENTICATION - Path: {}, User: {} (ID: {})",
                        path,
                        principal.email(),
                        principal.userId()
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + principal.role().name()
                            ))
                    );

            request.setAttribute(AUTH_SOURCE, "JWT_BEARER_TOKEN");
            request.setAttribute("JWT_USER_ID", principal.userId());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return true;

        } catch (Exception e) {
            LOGGER.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());

            if (isApiEndpoint && !allowOAuth2ForApi) {
                sendJwtRequired(response);
                return true;
            }
        }

        return false;
    }

    private boolean handleExistingAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            String path,
            boolean isApiEndpoint,
            boolean allowOAuth2ForApi
    ) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof UsernamePasswordAuthenticationToken token &&
                token.getPrincipal() instanceof UserPrincipal) {
            return true;
        }

        if (auth instanceof OAuth2AuthenticationToken oauthToken) {

            if (isApiEndpoint && !allowOAuth2ForApi) {
                sendOAuth2NotAllowed(response);
                return true;
            }

            authenticateOAuth2User(request, oauthToken, path);
            return true;
        }

        return false;
    }

    private void authenticateOAuth2User(
            HttpServletRequest request,
            OAuth2AuthenticationToken oauthToken,
            String path
    ) {

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String email = resolveEmail(oauthUser);
        if (email == null) {
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "🔵 OAUTH2 SESSION AUTHENTICATION - Path: {}, User: {} (ID: {})",
                    path,
                    user.getEmail(),
                    user.getUserId()
            );
        }

        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        ))
                );

        request.setAttribute(AUTH_SOURCE, "OAUTH2_SESSION");
        request.setAttribute("OAUTH2_USER_ID", user.getUserId());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void enforceFinalAuthentication(
            HttpServletResponse response,
            String path,
            String authHeader,
            boolean isApiEndpoint
    ) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {

            if (isApiEndpoint) {
                sendJwtRequired(response);
                return;
            }

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "⚠️ NO AUTHENTICATION - Path: {}, Authorization header present: {}",
                        path,
                        authHeader != null
                );
            }
        }
    }

    private void sendJwtRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON);
        response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"JWT Bearer token required\"}"
        );
    }

    private void sendOAuth2NotAllowed(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON);
        response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"Use /api/auth/token to get JWT\"}"
        );
    }

    private String resolveEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
