package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.AllowedUserRepository;
import com.questevent.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuthSuccessService extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AllowedUserRepository allowedUserRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = resolveEmail(oauthUser);

        // 1️⃣ Allowed user check
        if (email == null || allowedUserRepository.findByEmail(email).isEmpty()) {
            response.sendRedirect("/access-denied");
            return;
        }

        // 2️⃣ Create user if first login
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(oauthUser.getAttribute("name"));
            u.setGender("GENDER");
            u.setRole(Role.USER);
            u.setDepartment(Department.GENERAL);
            return userRepository.save(u);
        });

        // 3️⃣ Let Spring decide where to redirect
        try {
            super.onAuthenticationSuccess(request, response, authentication);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
