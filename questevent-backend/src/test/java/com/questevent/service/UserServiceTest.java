package com.questevent.service;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Jwt jwt() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("email", "test@company.com")
        );
    }

    private User user() {
        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@company.com");
        return user;
    }

    @Test
    void addUser_success() {
        User user = user();
        when(userRepository.save(user)).thenReturn(user);
        User saved = userService.addUser(user);
        assertEquals("Test User", saved.getName());
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user()));
        List<UserResponseDto> users = userService.getAllUsers();
        assertEquals(1, users.size());
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        UserResponseDto dto = userService.getUserById(1L);
        assertEquals("test@company.com", dto.email());
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void getCurrentUser_success() {
        when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(user()));
        UserResponseDto dto = userService.getCurrentUser(jwt());
        assertEquals("Test User", dto.name());
    }

    @Test
    void getCurrentUser_notFound() {
        when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> userService.getCurrentUser(jwt()));
    }

    @Test
    void updateCurrentUser_success() {
        User existing = user();
        User update = new User();
        update.setName("Updated");
        when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User updated = userService.updateCurrentUser(jwt(), update);
        assertEquals("Updated", updated.getName());
    }

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser(1L));
    }
}
