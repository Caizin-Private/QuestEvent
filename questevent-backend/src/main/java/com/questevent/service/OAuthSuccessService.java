package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        HttpSession session = request.getSession();

        String email = resolveEmail(oauthUser);

        // 1️⃣ Create user if first login (allow any OAuth2 user)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(oauthUser.getAttribute("name"));
            u.setGender("GENDER");
            u.setRole(Role.USER);
            u.setDepartment(Department.GENERAL);
            return userRepository.save(u);
        });

        // 2️⃣ Set userId in session for AuthController
        session.setAttribute("userId", user.getUserId());

        // 3️⃣ Redirect based on profile completion
        if (user.getDepartment() == Department.GENERAL && "GENDER".equals(user.getGender())) {
            response.sendRedirect("/complete-profile");
        } else {
            response.sendRedirect("/home");
        }
    }

    private String resolveEmail(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
