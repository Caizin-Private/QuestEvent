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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setName("Test");
        user.setEmail("User@test.com");
        user.setGender("Male");
        user.setRole(Role.USER);
    }

    @Test
    void getAllUsers_success() {

        Mockito.when(userRepository.findAll())
                .thenReturn(List.of(user));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("Test", users.get(0).getName());
    }

    @Test
    void getUserById_success() {

        Mockito.when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User foundUser = userService.getUserById(1L);

        assertEquals("User@test.com", foundUser.getEmail());
    }

    @Test
    void getUserById_notFound_shouldThrowException() {

        Mockito.when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserById(1L));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void addUser_success() {

        Mockito.when(userRepository.save(user))
                .thenReturn(user);

        User savedUser = userService.addUser(user);

        assertEquals("Test", savedUser.getName());

        Mockito.verify(userRepository).save(user);
        Mockito.verify(userWalletService).createWalletForUser(user);
    }


    @Test
    void updateUser_success() {

        User updatedUser = new User();
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@test.com");
        updatedUser.setGender("Male");
        updatedUser.setRole(Role.HOST);

        Mockito.when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(user);

        User result = userService.updateUser(1L, updatedUser);

        assertEquals("Updated Name", result.getName());
        assertEquals("updated@test.com", result.getEmail());
        assertEquals(Role.HOST, result.getRole());
    }

    @Test
    void deleteUser_success() {

        Mockito.doNothing()
                .when(userRepository)
                .deleteById(1L);

        userService.deleteUser(1L);

        Mockito.verify(userRepository).deleteById(1L);
    }

    @Test
    void getAuthorities_success() {

        List<SimpleGrantedAuthority> authorities =
                userService.getAuthorities(user);

        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
    }


    @Test
    void convertToDto_success() {

        UserResponseDto dto = userService.convertToDto(user);

        assertEquals(1L, dto.getUserId());
        assertEquals("Test", dto.getName());
        assertEquals("User@test.com", dto.getEmail());
        assertEquals(Role.USER, dto.getRole());
    }
}
