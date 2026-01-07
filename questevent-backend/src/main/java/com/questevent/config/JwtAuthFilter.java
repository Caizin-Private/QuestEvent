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

    /* ===================== CONSTANTS ===================== */


    private static final String AUTH_SOURCE = "AUTH_SOURCE";
    private static final String JWT_SOURCE = "JWT_BEARER_TOKEN";
    private static final String OAUTH2_SOURCE = "OAUTH2_SESSION";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public JwtAuthFilter(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /* ===================== FILTER ===================== */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RequestContext ctx = RequestContext.from(request);

        if (isFrameworkEndpoint(ctx.path())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (handleJwt(ctx, request, response, filterChain)) {
            return;
        }

        if (rejectMissingJwt(ctx, response)) {
            return;
        }

        if (ctx.isPublicApi()) {
            filterChain.doFilter(request, response);
            return;
        }

        handleOAuth2(ctx, request, response);

        if (rejectUnauthenticatedApi(ctx, response)) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    /* ===================== JWT ===================== */

    private boolean handleJwt(
            RequestContext ctx,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        if (!ctx.hasBearer()) {
            return false;
        }

        try {
            String token = ctx.authHeader().substring(7);

            if (!jwtService.validateToken(token)) {
                return false;
            }

            UserPrincipal principal = jwtService.extractUserPrincipal(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + principal.role().name()
                            ))
                    );

            request.setAttribute(AUTH_SOURCE, JWT_SOURCE);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ JWT AUTH - Path: {}, User: {}", ctx.path(), principal.email());
            chain.doFilter(request, response);
            return true;

        } catch (Exception e) {
            log.warn("Invalid JWT token for path {}: {}", ctx.path(), e.getMessage());
            if (requiresJwt(ctx)) {
                writeUnauthorized(response, "Valid JWT Bearer token required");
                return true;
            }
            return false;
        }
    }

    /* ===================== OAUTH2 ===================== */

    private void handleOAuth2(
            RequestContext ctx,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken oauthToken)) {
            return;
        }

        if (requiresJwt(ctx)) {
            writeUnauthorized(
                    response,
                    "API endpoints require JWT Bearer token. Use /api/auth/token first."
            );
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String email = resolveEmail(oauthUser);
        if (email == null) return;

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), user.getEmail(), user.getRole());

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        ))
                );

        request.setAttribute(AUTH_SOURCE, OAUTH2_SOURCE);
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        log.info("🔵 OAUTH2 AUTH - Path: {}, User: {}", ctx.path(), user.getEmail());
    }

    /* ===================== REJECTIONS ===================== */

    private boolean rejectMissingJwt(RequestContext ctx, HttpServletResponse response)
            throws IOException {

        if (requiresJwt(ctx) && !ctx.hasBearer()) {
            writeUnauthorized(response, "JWT Bearer token required");
            return true;
        }
        return false;
    }

    private boolean rejectUnauthenticatedApi(RequestContext ctx, HttpServletResponse response)
            throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (requiresJwt(ctx) && (auth == null || !auth.isAuthenticated())) {
            writeUnauthorized(response, "JWT Bearer token required");
            return true;
        }
        return false;
    }

    /* ===================== CONDITIONS ===================== */

    private boolean requiresJwt(RequestContext ctx) {
        return ctx.isApi() && !ctx.isPublicApi() && !ctx.allowOAuth2();
    }

    private boolean isFrameworkEndpoint(String path) {
        return path.startsWith("/oauth2") || path.startsWith("/login");
    }

    /* ===================== UTIL ===================== */

    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {

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

    /* ===================== CONTEXT ===================== */

    private record RequestContext(
            String path,
            boolean isApi,
            boolean isPublicApi,
            boolean allowOAuth2,
            String authHeader,
            boolean hasBearer
    ) {
        static RequestContext from(HttpServletRequest request) {
            String path = request.getRequestURI();
            String authHeader = request.getHeader("Authorization");

            return new RequestContext(
                    path,
                    path.startsWith("/api/"),
                    path.equals("/api/auth") || path.equals("/api/auth/"),
                    path.equals("/api/auth/token"),
                    authHeader,
                    authHeader != null && authHeader.startsWith("Bearer ")
            );
        }
    }
}
