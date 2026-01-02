package com.questevent.config;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public JwtAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/oauth2") || path.startsWith("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication currentAuth =
                SecurityContextHolder.getContext().getAuthentication();

        // ✅ Already converted → stop immediately
        if (currentAuth instanceof UsernamePasswordAuthenticationToken token &&
                token.getPrincipal() instanceof UserPrincipal) {

            filterChain.doFilter(request, response);
            return;
        }
        if (currentAuth instanceof OAuth2AuthenticationToken oauthToken) {

            OAuth2User oauthUser = oauthToken.getPrincipal();

            String email = resolveEmail(oauthUser);

            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

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
