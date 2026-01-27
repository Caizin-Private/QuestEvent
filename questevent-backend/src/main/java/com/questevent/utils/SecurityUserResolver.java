package com.questevent.utils;

import com.questevent.entity.User;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUserResolver {

    private final UserRepository userRepository;

    public User resolveUser(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        Jwt jwt = jwtAuth.getToken();
        String email = extractEmail(jwt);

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not registered")
                );
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException("No authentication found");
        }

        return resolveUser(authentication);
    }

    public String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        return extractEmail(jwtAuth.getToken());
    }

    public String getCurrentUserName() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        return jwtAuth.getToken().getClaimAsString("name");
    }

    private String extractEmail(Jwt jwt) {

        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) {
            email = jwt.getClaimAsString("email");
        }

        if (email == null) {
            throw new UnauthorizedException("No email in token");
        }

        return email;
    }
}
