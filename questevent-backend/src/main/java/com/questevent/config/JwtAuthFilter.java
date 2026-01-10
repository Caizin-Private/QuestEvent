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

    private static final String APPLICATION_JSON = "application/json";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

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

        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApi = path.startsWith("/api/");
        boolean isPublicApi = isPublicApi(path);
        boolean allowOAuth2 = isTokenEndpoint(path);

        String authHeader = request.getHeader(AUTH_HEADER);
        boolean hasBearer = authHeader != null && authHeader.startsWith(BEARER_PREFIX);

        if (hasBearer) {
            boolean authenticated = authenticateWithJwt(
                    authHeader, request, response, path, isApi, allowOAuth2
            );
            if (authenticated) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (isApi && requiresJwt(isPublicApi, allowOAuth2, hasBearer)) {
            writeUnauthorized(response, "JWT Bearer token required");
            return;
        }

        if (isPublicApi) {
            filterChain.doFilter(request, response);
            return;
        }

        if (handleExistingAuthentication(
                request, response, filterChain, path, isApi, allowOAuth2)) {
            return;
        }

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

    private boolean isPublicApi(String path) {
        return path.equals("/api/auth") || path.equals("/api/auth/");
    }

    private boolean isTokenEndpoint(String path) {
        return path.equals("/api/auth/token");
    }

    private boolean requiresJwt(boolean isPublicApi, boolean allowOAuth2, boolean hasBearer) {
        return !isPublicApi && !allowOAuth2 && !hasBearer;
    }

    private boolean authenticateWithJwt(
            String authHeader,
            HttpServletRequest request,
            HttpServletResponse response,
            String path,
            boolean isApi,
            boolean allowOAuth2
    ) throws IOException {

        String token = authHeader.substring(BEARER_PREFIX.length());
        UserPrincipal principal;

        try {
            if (!jwtService.validateToken(token)) {
                return false;
            }
            principal = jwtService.extractUserPrincipal(token);
        } catch (Exception ex) {
            log.warn("Invalid JWT token for path {}: {}", path, ex.getMessage());
            if (isApi && !allowOAuth2) {
                writeUnauthorized(response, "Valid JWT Bearer token required");
                return true;
            }
            return false;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + principal.role().name()
                        ))
                );

        request.setAttribute("AUTH_SOURCE", "JWT");
        request.setAttribute("JWT_USER_ID", principal.userId());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("JWT AUTH - Path: {}, User: {}", path, principal.email());

        return true;
    }

    private boolean handleExistingAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String path,
            boolean isApi,
            boolean allowOAuth2
    ) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof UsernamePasswordAuthenticationToken) {
            filterChain.doFilter(request, response);
            return true;
        }

        if (auth instanceof OAuth2AuthenticationToken oauthToken) {

            if (isApi && !allowOAuth2) {
                writeUnauthorized(response, "Use /api/auth/token to get JWT");
                return true;
            }

            authenticateFromOAuth(oauthToken, request, path);
            filterChain.doFilter(request, response);
            return true;
        }

        return false;
    }

    private void authenticateFromOAuth(
            OAuth2AuthenticationToken token,
            HttpServletRequest request,
            String path
    ) {

        OAuth2User oauthUser = token.getPrincipal();
        String email = resolveEmail(oauthUser);

        if (email == null) {
            return;
        }

        userRepository.findByEmail(email).ifPresent(user -> {

            UserPrincipal principal = new UserPrincipal(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole()
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + user.getRole().name()
                            ))
                    );

            request.setAttribute("AUTH_SOURCE", "OAUTH2");
            request.setAttribute("OAUTH2_USER_ID", user.getUserId());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("OAUTH2 AUTH - Path: {}, User: {}", path, user.getEmail());
        });
    }

    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON);
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
