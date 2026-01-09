package com.questevent.config;

import com.questevent.dto.UserPrincipal;
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

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String AUTH_SOURCE = "AUTH_SOURCE";
    private static final String AUTH_SOURCE_JWT = "JWT_BEARER_TOKEN";
    private static final String AUTH_SOURCE_OAUTH2 = "OAUTH2_SESSION";
    private static final String CONTENT_TYPE_JSON = "application/json";

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

        if (isPublicWebPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApi = isApiPath(path);
        boolean allowOAuth2ForApi = isTokenEndpoint(path);

        String authHeader = request.getHeader("Authorization");

        if (hasBearerToken(authHeader) &&
                authenticateWithJwt(authHeader, request, response, filterChain, path, isApi, allowOAuth2ForApi)) {
            return;
        }

        if (requiresJwtButMissing(isApi, path, allowOAuth2ForApi, authHeader)) {
            sendUnauthorized(response, "JWT Bearer token required in Authorization header for API endpoints");
            return;
        }

        if (isPublicApi(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (handleExistingAuthentication(request, response, filterChain, path, isApi, allowOAuth2ForApi)) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicWebPath(String path) {
        return path.startsWith("/oauth2") || path.startsWith("/login");
    }

    private boolean isApiPath(String path) {
        return path.startsWith("/api/");
    }

    private boolean isPublicApi(String path) {
        return path.equals("/api/auth") || path.equals("/api/auth/");
    }

    private boolean isTokenEndpoint(String path) {
        return path.equals("/api/auth/token");
    }

    private boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private boolean requiresJwtButMissing(
            boolean isApi,
            String path,
            boolean allowOAuth2ForApi,
            String authHeader
    ) {
        return isApi && !isPublicApi(path) && !allowOAuth2ForApi && !hasBearerToken(authHeader);
    }

    private boolean authenticateWithJwt(
            String authHeader,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            String path,
            boolean isApi,
            boolean allowOAuth2ForApi
    ) throws IOException, ServletException {

        try {
            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) return false;

            UserPrincipal principal = jwtService.extractUserPrincipal(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                    );

            request.setAttribute(AUTH_SOURCE, AUTH_SOURCE_JWT);
            request.setAttribute("JWT_USER_ID", principal.userId());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
            return true;

        } catch (Exception e) {
            log.warn("Invalid JWT token for path {}: {}", path, e.getMessage());

            if (isApi && !allowOAuth2ForApi) {
                sendUnauthorized(response, "Valid JWT Bearer token required for API endpoints");
                return true;
            }
            return false;
        }
    }

    private boolean handleExistingAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            String path,
            boolean isApi,
            boolean allowOAuth2ForApi
    ) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof UsernamePasswordAuthenticationToken) {
            chain.doFilter(request, response);
            return true;
        }

        if (auth instanceof OAuth2AuthenticationToken oauth) {
            if (isApi && !allowOAuth2ForApi) {
                sendUnauthorized(response, "API endpoints require JWT Bearer token");
                return true;
            }
            authenticateFromOAuth2(oauth, request);
            chain.doFilter(request, response);
            return true;
        }

        return false;
    }

    private void authenticateFromOAuth2(OAuth2AuthenticationToken oauth, HttpServletRequest request) {
        String email = resolveEmail(oauth.getPrincipal());
        if (email == null) return;

        userRepository.findByEmail(email).ifPresent(user -> {
            UserPrincipal principal =
                    new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );

            request.setAttribute(AUTH_SOURCE, AUTH_SOURCE_OAUTH2);
            request.setAttribute("OAUTH2_USER_ID", user.getUserId());

            SecurityContextHolder.getContext().setAuthentication(auth);
        });
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE_JSON);
        response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}"
        );
    }

    private String resolveEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
