package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthController authController;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setName("Test");
        user.setEmail("User@test.com");
        user.setGender("Male");
        user.setDepartment(user.getDepartment());
        user.setRole(user.getRole());
    }

    @Test
    void home_shouldReturnLoginSuccessHtml() {

        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(session.getAttribute("userId")).thenReturn(1L);

        String response = authController.home(request);

        assertTrue(response.contains("Login Successful"));
        assertTrue(response.contains("UserID = 1"));
        assertTrue(response.contains("Logout"));
    }


    @Test
    void getProfile_shouldReturnUserProfileMap() {

        Mockito.when(request.getSession()).thenReturn(session);
        Mockito.when(session.getAttribute("userId")).thenReturn(1L);
        Mockito.when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Map<String, Object> response =
                authController.getProfile(request);

        assertEquals(1L, response.get("userId"));
        assertEquals("Test", response.get("name"));
        assertEquals("User@test.com", response.get("email"));
        assertEquals("Male", response.get("gender"));
    }


    @Test
    void logoutSuccess_shouldReturnLogoutHtml() {

        String response = authController.logoutSuccess();

        assertTrue(response.contains("Logged out successfully"));
        assertTrue(response.contains("Login again"));
    }
}
