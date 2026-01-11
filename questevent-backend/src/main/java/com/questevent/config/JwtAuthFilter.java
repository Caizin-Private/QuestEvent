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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String APPLICATION_JSON = "application/json";
    private static final String AUTH_SOURCE = "AUTH_SOURCE";
    private static final String JWT_BEARER_TOKEN = "JWT_BEARER_TOKEN";
    private static final String OAUTH2_SESSION = "OAUTH2_SESSION";

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

        if (isSkippedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApi = path.startsWith("/api/");
        boolean allowOAuth2ForApi = path.equals("/api/auth/token");
        boolean isPublicApi = path.equals("/api/auth") || path.equals("/api/auth/");

        if (tryJwtAuthentication(request, response, filterChain, path, isApi, allowOAuth2ForApi)) {
            return;
        }

        if (isApi && !isPublicApi && !allowOAuth2ForApi && !hasBearerToken(request)) {
            writeUnauthorized(response, "JWT Bearer token required");
            return;
        }

        if (isPublicApi) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isUserAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tryOAuth2Authentication(request, response, isApi, allowOAuth2ForApi)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isApi && !isAuthenticated()) {
            writeUnauthorized(response, "JWT Bearer token required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSkippedPath(String path) {
        return path.startsWith("/oauth2") ||
                path.startsWith("/login") ||
                path.startsWith("/error") ||
                path.startsWith("/css") ||
                path.startsWith("/js") ||
                path.startsWith("/images") ||
                path.startsWith("/favicon");
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private boolean tryJwtAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String path,
            boolean isApi,
            boolean allowOAuth2ForApi
    ) throws IOException, ServletException {

        if (!hasBearerToken(request)) return false;

        String token = request.getHeader("Authorization").substring(7);

        try {
            if (!jwtService.validateToken(token)) return false;

            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

            if (existingAuth == null) {

                UserPrincipal userPrincipal = jwtService.extractUserPrincipal(token);

                String roleName = "ROLE_" + userPrincipal.role().name();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal,
                                null,
                                List.of(new SimpleGrantedAuthority(roleName))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("✅ JWT AUTH - Path: {}, User: {} (ID: {})",
                        path, userPrincipal.email(), userPrincipal.userId());
            }

            request.setAttribute(AUTH_SOURCE, JWT_BEARER_TOKEN);
            request.setAttribute("JWT_USER_ID", getUserIdFromContext());

            filterChain.doFilter(request, response);
            return true;

        } catch (Exception e) {
            log.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());
            if (isApi && !allowOAuth2ForApi) {
                writeUnauthorized(response, "Valid JWT Bearer token required");
                return true;
            }
            return false;
        }
    }

    private Long getUserIdFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private boolean isUserAlreadyAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getPrincipal() instanceof UserPrincipal;
    }

    private boolean tryOAuth2Authentication(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean isApi,
            boolean allowOAuth2ForApi
    ) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof OAuth2AuthenticationToken oauthToken)) return false;

        if (isApi && !allowOAuth2ForApi) {
            writeUnauthorized(response, "Use /api/auth/token to get JWT");
            return true;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String email = resolveEmail(oauthUser);
        if (email == null) return false;

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        log.info("🔵 OAUTH2 SESSION AUTH - User: {} (ID: {})",
                user.getEmail(), user.getUserId());

        UserPrincipal principal = new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getRole()
        );

        String roleName = "ROLE_" + user.getRole().name();

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(roleName))
                );

        request.setAttribute(AUTH_SOURCE, OAUTH2_SESSION);
        request.setAttribute("OAUTH2_USER_ID", user.getUserId());

        SecurityContextHolder.getContext().setAuthentication(newAuth);
        return true;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
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
