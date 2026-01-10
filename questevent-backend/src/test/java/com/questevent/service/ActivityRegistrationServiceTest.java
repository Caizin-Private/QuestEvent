package com.questevent.service;

import com.questevent.dto.AddParticipantInActivityRequestDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.exception.ActivityNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityRegistrationServiceTest {

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityRegistrationService activityRegistrationService;

    private void authenticate(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        Authentication auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerParticipantForActivity_success() {
        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        authenticate(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        ActivityRegistration saved = new ActivityRegistration();
        saved.setUser(user);
        saved.setActivity(activity);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenReturn(saved);

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.registerParticipantForActivity(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(activityId, response.getActivityId());

        verify(activityRegistrationRepository).save(any());

        clearAuth();
    }

    @Test
    void registerParticipantForActivity_userNotFound() {
        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        authenticate(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User not found", ex.getMessage());

        clearAuth();
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {
        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        authenticate(userId);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        ActivityNotFoundException ex = assertThrows(
                ActivityNotFoundException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("Activity not found", ex.getMessage());

        clearAuth();
    }

    @Test
    void registerParticipantForActivity_alreadyRegistered() {
        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        authenticate(userId);

        User user = new User();
        user.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId))
                .thenReturn(true);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals(
                "User already registered for this activity",
                ex.getMessage()
        );

        clearAuth();
    }

    @Test
    void addParticipantToActivity_success() {
        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        ActivityRegistration saved = new ActivityRegistration();
        saved.setUser(user);
        saved.setActivity(activity);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenReturn(saved);

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.addParticipantToActivity(activityId, request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());

        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void deleteRegistration_success() {
        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                activityRegistrationService.deleteRegistration(registrationId));

        verify(activityRegistrationRepository).deleteById(registrationId);
    }

    @Test
    void deleteRegistration_notFound() {
        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> activityRegistrationService.deleteRegistration(registrationId)
        );

        assertEquals("Registration not found", ex.getMessage());
    }
}
