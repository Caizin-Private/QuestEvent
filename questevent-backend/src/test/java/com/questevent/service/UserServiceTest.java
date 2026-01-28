package com.questevent.service;

import com.questevent.dto.CompleteProfileRequest;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("test@questevent.com");
        user.setName("Test User");
    }

    @Test
    void addUser_shouldSaveUserAndCreateWallet() {
        // Arrange
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User savedUser = userService.addUser(user);

        // Assert
        assertEquals(user, savedUser);

        verify(userRepository, times(1)).save(user);
        verifyNoInteractions(userWalletService);
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDto dto = userService.getUserById(1L);

        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.email()).isEqualTo("test@questevent.com");
    }

    @Test
    void getUserById_fails_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getCurrentUser_success() {
        when(securityUserResolver.getCurrentUser()).thenReturn(user);

        UserResponseDto dto = userService.getCurrentUser();

        assertThat(dto.userId()).isEqualTo(user.getUserId());
        assertThat(dto.email()).isEqualTo(user.getEmail());
    }

    @Test
    void updateCurrentUser_updatesSafeFieldsOnly() {
        User updated = new User();
        updated.setName("Updated Name");
        updated.setDepartment(Department.IT);
        updated.setGender("F");

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateCurrentUser(updated);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDepartment()).isEqualTo(Department.IT);
        assertThat(result.getGender()).isEqualTo("F");
    }

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_fails_whenNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void completeProfile_updatesExistingUser_whenProfileIncomplete() {

        CompleteProfileRequest request =
                new CompleteProfileRequest(Department.HR, "M");

        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.completeProfile(request);

        assertThat(result.getDepartment()).isEqualTo(Department.HR);
        assertThat(result.getGender()).isEqualTo("M");

        verify(userRepository).save(user);
        verifyNoInteractions(userWalletService);
    }

    @Test
    void completeProfile_overwritesExistingProfile_whenAlreadyCompleted() {

        user.setDepartment(Department.IT);
        user.setGender("F");

        CompleteProfileRequest request =
                new CompleteProfileRequest(Department.HR, "M");

        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.completeProfile(request);

        assertThat(result.getDepartment()).isEqualTo(Department.HR);
        assertThat(result.getGender()).isEqualTo("M");

        verify(userRepository).save(user);
        verifyNoInteractions(userWalletService);
    }
}
