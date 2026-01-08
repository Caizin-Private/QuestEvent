package com.questevent.service;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setGender("Male");
        user.setRole(Role.USER);
    }

    @Test
    void shouldReturnAllUsers_whenUsersExist() {

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("Test User", users.get(0).getName());
        verify(userRepository).findAll();
    }

    @Test
    void shouldReturnUser_whenValidIdProvided() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        User found = userService.getUserById(userId);

        assertNotNull(found);
        assertEquals("user@test.com", found.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowException_whenUserNotFoundById() {

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(userId)
        );

        assertTrue(ex.getMessage().contains("User not found"));
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldSaveUserAndCreateWallet_whenAddUserCalled() {

        when(userRepository.save(user))
                .thenReturn(user);

        User saved = userService.addUser(user);

        assertNotNull(saved);
        assertEquals("Test User", saved.getName());

        verify(userRepository).save(user);
        verify(userWalletService).createWalletForUser(user);
    }

    @Test
    void shouldUpdateUserFields_whenValidUpdateProvided() {

        User updatedUser = new User();
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@test.com");
        updatedUser.setGender("Male");
        updatedUser.setRole(Role.HOST);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(userId, updatedUser);

        assertEquals("Updated Name", result.getName());
        assertEquals("updated@test.com", result.getEmail());
        assertEquals(Role.HOST, result.getRole());

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void shouldDeleteUser_whenDeleteUserCalled() {

        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void shouldReturnUserRoleAuthority() {

        List<SimpleGrantedAuthority> authorities =
                userService.getAuthorities(user);

        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
    }

    @Test
    void shouldConvertUserToDtoCorrectly() {

        UserResponseDto dto = userService.convertToDto(user);

        assertNotNull(dto);
        assertEquals(userId, dto.getUserId());
        assertEquals("Test User", dto.getName());
        assertEquals("user@test.com", dto.getEmail());
        assertEquals(Role.USER, dto.getRole());
    }
}
