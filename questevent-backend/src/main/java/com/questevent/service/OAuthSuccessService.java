package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
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

        log.debug("OAuth2 authentication success triggered");

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        HttpSession session = request.getSession();

        String email = resolveEmail(oauthUser);

        log.debug("Resolved OAuth email | email={}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {

            log.info("New OAuth user detected | email={}", email);

            User u = new User();
            u.setEmail(email);
            u.setName(oauthUser.getAttribute("name"));
            u.setRole(Role.USER);

            u.setDepartment(Department.GENERAL);
            u.setGender("PENDING");

            return userRepository.save(u);
        });

        log.info(
                "OAuth login successful | userId={} | email={}",
                user.getUserId(),
                user.getEmail()
        );

        session.setAttribute("userId", user.getUserId());

        boolean profileIncomplete =
                user.getDepartment() == Department.GENERAL &&
                        "PENDING".equals(user.getGender());
        if (profileIncomplete) {
            log.info(
                    "Redirecting OAuth user to profile completion | userId={}",
                    user.getUserId()
            );
            response.sendRedirect("/complete-profile");
        } else {
            log.info(
                    "Redirecting OAuth user to profile | userId={}",
                    user.getUserId()
            );
            response.sendRedirect("/profile");
        }
    }

    private String resolveEmail(OAuth2User oauthUser) {
        log.debug("Resolving email from OAuth2User attributes");
        String email = oauthUser.getAttribute("email");
        if (email == null) email = oauthUser.getAttribute("preferred_username");
        if (email == null) email = oauthUser.getAttribute("upn");
        return email;
    }
}
