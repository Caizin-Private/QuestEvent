package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

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
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("user@test.com");
    }

    /* ---------------- getAllUsers ---------------- */

    @Test
    void shouldReturnAllUsers_whenUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> users = userService.getAllUsers();

        assertEquals(1, users.size());
        verify(userRepository).findAll();
    }

    /* ---------------- getUser ---------------- */

    @Test
    void shouldReturnCurrentUser_whenAuthenticated() {
        mockSecurityContext(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        User result = userService.getUser();

        assertNotNull(result);
        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    void shouldThrowUserNotFoundException_whenAuthenticatedUserMissing() {
        mockSecurityContext(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser());

        verify(userRepository).findById(userId);
    }

    /* ---------------- addUser ---------------- */

    @Test
    void shouldSaveUserAndCreateWallet() {
        userService.addUser(user);

        verify(userRepository).save(user);
        verify(userWalletService).createWalletForUser(user);
    }

    /* ---------------- updateUser ---------------- */

    @Test
    void shouldUpdateCurrentUser() {
        mockSecurityContext(userId);

        User updated = new User();
        updated.setName("Updated");
        updated.setEmail("updated@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(updated);

        assertEquals("Updated", result.getName());
        verify(userRepository).save(user);
    }

    /* ---------------- deleteUser ---------------- */

    @Test
    void shouldDeleteUser() {
        userService.deleteUser(userId);
        verify(userRepository).deleteById(userId);
    }

    /* ---------------- getAuthorities ---------------- */

    @Test
    void shouldReturnAuthorities() {
        user.setRole(Role.USER);

        List<SimpleGrantedAuthority> authorities =
                userService.getAuthorities(user);

        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
    }

    /* ---------------- convertToDto ---------------- */

    @Test
    void shouldConvertUserToDto() {
        UserResponseDto dto = userService.convertToDto(user);

        assertEquals(userId, dto.getUserId());
        assertEquals("Test User", dto.getName());
    }

    /* ---------------- helper ---------------- */

    private void mockSecurityContext(Long userId) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal())
                .thenReturn(new UserPrincipal(userId, "user@test.com", Role.USER));
        when(ctx.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

}
