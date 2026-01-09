package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

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
    private OAuthSuccessService successService;

    @BeforeEach
    void setUp() {
        when(request.getSession()).thenReturn(session);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
    }

    @Test
    void shouldCreateNewUserAndRedirectToCompleteProfile_whenUserNotExists() throws IOException {

        when(oauth2User.getAttribute("email")).thenReturn("new@test.com");
        when(oauth2User.getAttribute("name")).thenReturn("New User");
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setEmail("new@test.com");
        savedUser.setDepartment(Department.GENERAL);
        savedUser.setGender("PENDING");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        successService.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any(User.class));
        verify(session).setAttribute("userId", 1L);
        verify(response).sendRedirect("/complete-profile");
    }

    @Test
    void shouldRedirectToCompleteProfile_whenExistingUserProfileIncomplete() throws IOException {

        when(oauth2User.getAttribute("email")).thenReturn("user@test.com");

        User user = new User();
        user.setUserId(2L);
        user.setDepartment(Department.GENERAL);
        user.setGender("PENDING");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        successService.onAuthenticationSuccess(request, response, authentication);

        verify(session).setAttribute("userId", 2L);
        verify(response).sendRedirect("/complete-profile");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRedirectToProfile_whenExistingUserProfileComplete() throws IOException {

        when(oauth2User.getAttribute("email")).thenReturn("user2@test.com");

        User user = new User();
        user.setUserId(3L);
        user.setDepartment(Department.TECH);
        user.setGender("MALE");

        when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(user));

        successService.onAuthenticationSuccess(request, response, authentication);

        verify(session).setAttribute("userId", 3L);
        verify(response).sendRedirect("/profile");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldResolveEmailFromPreferredUsername_whenEmailIsNull() throws IOException {

        when(oauth2User.getAttribute("email")).thenReturn(null);
        when(oauth2User.getAttribute("preferred_username")).thenReturn("alt@test.com");

        User user = new User();
        user.setUserId(4L);
        user.setDepartment(Department.TECH);
        user.setGender("MALE");

        when(userRepository.findByEmail("alt@test.com")).thenReturn(Optional.of(user));

        successService.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/profile");
    }

    @Test
    void shouldResolveEmailFromUpn_whenOthersAreNull() throws IOException {

        when(oauth2User.getAttribute("email")).thenReturn(null);
        when(oauth2User.getAttribute("preferred_username")).thenReturn(null);
        when(oauth2User.getAttribute("upn")).thenReturn("upn@test.com");

        User user = new User();
        user.setUserId(5L);
        user.setDepartment(Department.TECH);
        user.setGender("MALE");

        when(userRepository.findByEmail("upn@test.com")).thenReturn(Optional.of(user));

        successService.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/profile");
    }
}
