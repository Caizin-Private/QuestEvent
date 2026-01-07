package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @Mock
    private HttpSession session;

    @InjectMocks
    private OAuthSuccessService oAuthSuccessService;

    @BeforeEach
    void setup() {
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    void shouldNotCreateUserIfAlreadyExists() throws Exception {

        String email = "test@example.com";

        User existing = new User();
        existing.setUserId(10L);
        existing.setEmail(email);

        when(oauth2User.getAttribute("email")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        verify(session).setAttribute("userId", 10L);
        verify(response).sendRedirect("/profile");
    }

    @Test
    void shouldCreateUserIfNotExists() throws Exception {

        String email = "new@example.com";

        when(oauth2User.getAttribute("email")).thenReturn(email);
        when(oauth2User.getAttribute("name")).thenReturn("Test User");
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(20L);
        savedUser.setEmail(email);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User created = userCaptor.getValue();

        assertEquals(email, created.getEmail());
        assertEquals("Test User", created.getName());
        assertEquals(Role.USER, created.getRole());
        assertEquals(Department.GENERAL, created.getDepartment());
        assertEquals("GENDER", created.getGender());

        verify(session).setAttribute("userId", 20L);
        verify(response).sendRedirect("/profile");
    }

    @Test
    void shouldResolveEmailFromPreferredUsername() throws Exception {

        when(oauth2User.getAttribute("email")).thenReturn(null);
        when(oauth2User.getAttribute("preferred_username"))
                .thenReturn("fallback@test.com");

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(30L);

        when(userRepository.save(any())).thenReturn(savedUser);

        oAuthSuccessService.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any(User.class));
        verify(session).setAttribute("userId", 30L);
        verify(response).sendRedirect("/profile");
    }
}
