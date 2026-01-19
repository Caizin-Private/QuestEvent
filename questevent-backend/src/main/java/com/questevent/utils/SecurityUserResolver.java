package com.questevent.utils;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.exception.UnauthorizedException;
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

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        Jwt jwt = jwtAuth.getToken();

        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) {
            email = jwt.getClaimAsString("email");
        }
        if (email == null) {
            throw new UnauthorizedException("No email in token");
        }

        final String finalEmail = email;

        return userRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(finalEmail);
                    u.setName(jwt.getClaimAsString("name"));
                    u.setRole(Role.USER);
                    return userRepository.save(u);
                });
    }
}

