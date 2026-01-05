package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.io.IOException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OAuthSuccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @InjectMocks
    private OAuthSuccessService oAuthSuccessService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("test@company.com");
        user.setName("Test User");
        user.setGender("GENDER");
        user.setDepartment(Department.GENERAL);
        user.setRole(Role.USER);
    }
    @Test
    void shouldRedirectToCompleteProfile_whenUserHasDefaultProfile() throws IOException {

        Mockito.when(authentication.getPrincipal()).thenReturn(oauth2User);
        Mockito.when(request.getSession()).thenReturn(session);

        Mockito.when(oauth2User.getAttribute("email"))
                .thenReturn("test@company.com");

        Mockito.when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(user));

        oAuthSuccessService.onAuthenticationSuccess(
                request, response, authentication);

        Mockito.verify(session)
                .setAttribute("userId", 1L);

        Mockito.verify(response)
                .sendRedirect("/complete-profile");
    }

    @Test
    void shouldRedirectToHome_whenProfileIsComplete() throws IOException {

        user.setGender("Male");
        user.setDepartment(Department.IT);

        Mockito.when(authentication.getPrincipal()).thenReturn(oauth2User);
        Mockito.when(request.getSession()).thenReturn(session);

        Mockito.when(oauth2User.getAttribute("email"))
                .thenReturn("test@company.com");

        Mockito.when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(user));

        oAuthSuccessService.onAuthenticationSuccess(
                request, response, authentication);

        Mockito.verify(response)
                .sendRedirect("/home");
    }

    @Test
    void shouldCreateUser_whenUserDoesNotExist() throws IOException {

        Mockito.when(authentication.getPrincipal()).thenReturn(oauth2User);
        Mockito.when(request.getSession()).thenReturn(session);

        Mockito.when(oauth2User.getAttribute("email"))
                .thenReturn("new@company.com");

        Mockito.when(oauth2User.getAttribute("name"))
                .thenReturn("New User");

        Mockito.when(userRepository.findByEmail("new@company.com"))
                .thenReturn(Optional.empty());

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setUserId(10L);
                    return saved;
                });

        oAuthSuccessService.onAuthenticationSuccess(
                request, response, authentication);

        Mockito.verify(userRepository)
                .save(Mockito.any(User.class));

        Mockito.verify(session)
                .setAttribute("userId", 10L);

        Mockito.verify(response)
                .sendRedirect("/complete-profile");
    }

    @Test
    void shouldResolveEmail_fromPreferredUsername() throws IOException {

        Mockito.when(authentication.getPrincipal()).thenReturn(oauth2User);
        Mockito.when(request.getSession()).thenReturn(session);

        Mockito.when(oauth2User.getAttribute("email"))
                .thenReturn(null);
        Mockito.when(oauth2User.getAttribute("preferred_username"))
                .thenReturn("fallback@company.com");

        Mockito.when(userRepository.findByEmail("fallback@company.com"))
                .thenReturn(Optional.of(user));

        oAuthSuccessService.onAuthenticationSuccess(
                request, response, authentication);

        Mockito.verify(response)
                .sendRedirect("/complete-profile");
    }
}
