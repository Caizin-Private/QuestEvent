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

    private void mockAuthenticatedUser(Long userId) {
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
        mockAuthenticatedUser(1L);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");
        activity.setRewardGems(100);

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(1L);
        saved.setActivity(activity);
        saved.setUser(user);
        saved.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.registerParticipantForActivity(request);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getActivityId());
        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void registerParticipantForActivity_userNotFound() {
        mockAuthenticatedUser(1L);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {
        mockAuthenticatedUser(1L);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(99L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("Activity not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_alreadyRegistered() {
        mockAuthenticatedUser(1L);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);

        User user = new User();
        user.setUserId(1L);

        Activity activity = new Activity();
        activity.setActivityId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(1L, 1L))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User already registered for this activity", ex.getMessage());
    }


    @Test
    void addParticipantToActivity_success() {
        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(2L);

        Activity activity = new Activity();
        activity.setActivityId(1L);

        User user = new User();
        user.setUserId(2L);
        user.setName("Target User");

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(1L);
        saved.setActivity(activity);
        saved.setUser(user);
        saved.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(1L, 2L))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.addParticipantToActivity(1L, request);

        assertNotNull(result);
        assertEquals(2L, result.getUserId());
        verify(activityRegistrationRepository).save(any());
    }


    @Test
    void deleteRegistration_success() {
        when(activityRegistrationRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() ->
                activityRegistrationService.deleteRegistration(1L));
    }

    @Test
    void deleteRegistration_notFound() {
        when(activityRegistrationRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.deleteRegistration(99L)
        );

        assertEquals("Registration not found", ex.getMessage());
    }
}
