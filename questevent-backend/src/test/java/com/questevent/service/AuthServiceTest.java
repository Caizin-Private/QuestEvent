package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Department;
import com.questevent.exception.UnauthenticatedUserException;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");
    }

    @Test
    void isLoggedIn_returnsTrue_whenSessionHasUserId() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);

        assertTrue(authService.isLoggedIn(request));
    }

    @Test
    void isLoggedIn_returnsFalse_whenSessionMissing() {
        when(request.getSession(false)).thenReturn(null);

        assertFalse(authService.isLoggedIn(request));
    }

    @Test
    void getLoggedInUserId_throwsException_whenNotLoggedIn() {
        when(request.getSession(false)).thenReturn(null);

        assertThrows(
                UnauthenticatedUserException.class,
                () -> authService.getLoggedInUserId(request)
        );
    }

    @Test
    void getLoggedInUser_returnsUser_whenPresent() {
        User user = new User();
        user.setUserId(1L);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = authService.getLoggedInUser(request);

        assertEquals(1L, result.getUserId());
    }

    @Test
    void shouldReturnTrueWhenUserIsLoggedIn() {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);

        boolean result = authService.isLoggedIn(request);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenSessionIsNull() {

        when(request.getSession(false)).thenReturn(null);

        boolean result = authService.isLoggedIn(request);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenUserIdNotInSession() {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(null);

        boolean result = authService.isLoggedIn(request);

        assertFalse(result);
    }

    @Test
    void shouldReturnUserIdWhenLoggedIn() {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);

        Long userId = authService.getLoggedInUserId(request);

        assertEquals(1L, userId);
    }

    @Test
    void shouldThrowWhenSessionMissingInGetUserId() {

        when(request.getSession(false)).thenReturn(null);

        RuntimeException ex =
                assertThrows(RuntimeException.class,
                        () -> authService.getLoggedInUserId(request));

        assertEquals("User not logged in", ex.getMessage());
    }

    @Test
    void shouldReturnLoggedInUser() {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = authService.getLoggedInUser(request);

        assertEquals(1L, result.getUserId());
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex =
                assertThrows(RuntimeException.class,
                        () -> authService.getLoggedInUser(request));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldUpdateProfileAndCreateWalletIfMissing() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        when(userWalletRepository.save(any(UserWallet.class)))
                .thenAnswer(i -> i.getArgument(0));

        authService.completeProfile(1L, Department.IT, "MALE");

        assertEquals(Department.IT, user.getDepartment());
        assertEquals("MALE", user.getGender());

        verify(userRepository).save(user);
        verify(userWalletRepository).save(any(UserWallet.class));
    }

    @Test
    void shouldUpdateProfileWithoutCreatingWalletIfExists() {

        UserWallet wallet = new UserWallet();
        wallet.setUser(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(wallet));

        authService.completeProfile(1L, Department.HR, "FEMALE");

        assertEquals(Department.HR, user.getDepartment());
        assertEquals("FEMALE", user.getGender());

        verify(userRepository).save(user);
        verify(userWalletRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundDuringProfileUpdate() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex =
                assertThrows(RuntimeException.class,
                        () -> authService.completeProfile(1L, Department.IT, "MALE"));

        assertEquals("User not found", ex.getMessage());
    }
}
