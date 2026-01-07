package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthSuccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuthSuccessService oAuthSuccessService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Test
    void shouldRedirectToCompleteProfileForNewUser() throws IOException {

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("test@gmail.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");

        when(request.getSession()).thenReturn(session);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setEmail("test@gmail.com");
        savedUser.setRole(Role.USER);
        savedUser.setDepartment(Department.GENERAL);
        savedUser.setGender("PENDING");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        verify(session).setAttribute("userId", 1L);
        verify(response).sendRedirect("/complete-profile");
    }

    @Test
    void shouldRedirectToProfileIfProfileCompleted() throws IOException {

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("user@gmail.com");

        when(request.getSession()).thenReturn(session);

        User existingUser = new User();
        existingUser.setUserId(2L);
        existingUser.setEmail("user@gmail.com");
        existingUser.setDepartment(Department.TECH);
        existingUser.setGender("MALE");

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(existingUser));

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        verify(session).setAttribute("userId", 2L);
        verify(response).sendRedirect("/profile");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldResolveEmailFromPreferredUsername() throws IOException {

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("preferred_username"))
                .thenReturn("azure@user.com");

        when(request.getSession()).thenReturn(session);

        when(userRepository.findByEmail("azure@user.com"))
                .thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(3L);
        savedUser.setDepartment(Department.GENERAL);
        savedUser.setGender("PENDING");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/complete-profile");
    }
}
