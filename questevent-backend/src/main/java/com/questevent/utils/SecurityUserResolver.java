package com.questevent.utils;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.exception.UnauthorizedException;
import com.questevent.repository.UserRepository;
import com.questevent.service.UserWalletService;
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
    private final UserWalletService userWalletService;

    public User resolveUser(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedException("Invalid authentication");
        }

        Jwt jwt = jwtAuth.getToken();

        String email = extractEmail(jwt);
        String name  = jwt.getClaimAsString("name");

        return userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User user = new User();
                    user.setEmail(email);
                    user.setName(name);
                    user.setRole(Role.USER);

                    user.setDepartment(Department.GENERAL);
                    user.setGender("Not Specified");

                    User savedUser = userRepository.save(user);
                    userWalletService.createWalletForUser(savedUser);

                    return savedUser;
                });
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
