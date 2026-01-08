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

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthFilter.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public JwtAuthFilter(
            UserRepository userRepository,
            JwtService jwtService
    ) {
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

        // Allow OAuth endpoints
        if (path.startsWith("/oauth2") || path.startsWith("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isApi = path.startsWith("/api/");
        boolean isPublicApi = path.equals("/api/auth") || path.equals("/api/auth/");
        boolean isTokenEndpoint = path.equals("/api/auth/token");
        boolean allowOAuthForApi = isTokenEndpoint;

        String authHeader = request.getHeader("Authorization");
        boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");

        /* ================= JWT AUTH ================= */

        if (hasBearer) {
            String token = authHeader.substring(7);

            try {
                if (jwtService.validateToken(token)) {

                    UserPrincipal principal =
                            jwtService.extractUserPrincipal(token);

                    logger.info(
                            "✅ JWT AUTH - Path: {}, User: {} (ID: {})",
                            path, principal.email(), principal.userId()
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + principal.role().name()
                                    ))
                            );

                    // ✅ IMPORTANT: allow RbacService to cache user
                    authentication.setDetails(principal);

                    request.setAttribute("AUTH_SOURCE", "JWT");
                    request.setAttribute("JWT_USER_ID", principal.userId());

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                logger.warn("❌ Invalid JWT - {}", e.getMessage());
            }
        }

        /* ================= API SECURITY ================= */

        if (isApi && !isPublicApi && !allowOAuthForApi && !hasBearer) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Unauthorized","message":"JWT Bearer token required"}
                    """);
            return;
        }

        /* ================= OAUTH2 SESSION ================= */

        Authentication currentAuth =
                SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth instanceof OAuth2AuthenticationToken oauthToken) {

            if (isApi && !allowOAuthForApi) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                        {"error":"Unauthorized","message":"API requires JWT token"}
                        """);
                return;
            }

            OAuth2User oauthUser = oauthToken.getPrincipal();
            String email = resolveEmail(oauthUser);

            if (email != null) {
                Optional<User> userOpt =
                        userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    logger.info(
                            "🔵 OAUTH2 AUTH - Path: {}, User: {} (ID: {})",
                            path, user.getEmail(), user.getUserId()
                    );

                    UserPrincipal principal =
                            new UserPrincipal(
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

                    // ✅ IMPORTANT: allow RbacService to cache user
                    newAuth.setDetails(principal);

                    request.setAttribute("AUTH_SOURCE", "OAUTH2");
                    request.setAttribute("OAUTH2_USER_ID", user.getUserId());

                    SecurityContextHolder.getContext()
                            .setAuthentication(newAuth);
                }
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
