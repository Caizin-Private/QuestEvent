package com.questevent.utils;

import com.questevent.dto.UserPrincipal;
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

    public UserPrincipal getCurrentUserPrincipal() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        Jwt jwt = jwtAuth.getToken();

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new UnauthorizedException("Invalid token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
