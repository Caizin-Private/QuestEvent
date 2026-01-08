package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.Role;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

class ActivityRegistrationServiceTest {

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityRegistrationService activityRegistrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                "test@questevent.com",
                Role.USER
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void registerParticipantForActivity_success() {

        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test Activity");
        activity.setRewardGems(100L);

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(registrationId);
        saved.setActivity(activity);
        saved.setUser(user);
        saved.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any())).thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.registerParticipantForActivity(request);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(activityId, result.getActivityId());
        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void registerParticipantForActivity_userNotFound() {

        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {

        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("Activity not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_alreadyRegistered() {

        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User already registered for this activity", ex.getMessage());
    }

    @Test
    void addParticipantToActivity_success() {

        UUID activityId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(targetUserId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        User user = new User();
        user.setUserId(targetUserId);
        user.setName("Target User");

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(registrationId);
        saved.setActivity(activity);
        saved.setUser(user);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, targetUserId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any())).thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.addParticipantToActivity(activityId, request);

        assertNotNull(result);
        assertEquals(targetUserId, result.getUserId());
        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void deleteRegistration_success() {

        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                activityRegistrationService.deleteRegistration(registrationId));
    }

    @Test
    void deleteRegistration_notFound() {

        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.deleteRegistration(registrationId)
        );

        assertEquals("Registration not found", ex.getMessage());
    }
}
