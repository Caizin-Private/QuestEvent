package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.Role;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private static final Long USER_ID = 1L;

    /* ===================== Security Context Setup ===================== */

    @BeforeEach
    void setUpSecurityContext() {

        UserPrincipal principal = new UserPrincipal(
                USER_ID,
                "test@example.com",
                Role.USER
        );

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /* ===================== Tests ===================== */

    @Test
    void submitActivity_shouldSaveSubmissionSuccessfully() {

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(registrationId))
                .thenReturn(false);

        submissionService.submitActivity(
                activityId,
                "https://github.com/project"
        );

        verify(submissionRepository).save(any(ActivitySubmission.class));
        verify(registrationRepository).save(registration);

        assertEquals(
                CompletionStatus.COMPLETED,
                registration.getCompletionStatus()
        );
    }

    @Test
    void submitActivity_shouldThrowIfUserNotRegistered() {

        UUID activityId = UUID.randomUUID();

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        "url"
                )
        );

        assertEquals(
                "User is not registered for this activity",
                ex.getMessage()
        );

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitActivity_shouldThrowIfActivityAlreadyCompleted() {

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        "url"
                )
        );

        assertEquals(
                "Activity already completed. Submission not allowed.",
                ex.getMessage()
        );

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitActivity_shouldThrowIfSubmissionAlreadyExists() {

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(registrationId))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        "url"
                )
        );

        assertEquals(
                "Submission already exists for this registration",
                ex.getMessage()
        );

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitActivity_shouldThrowIfPrincipalIsInvalid() {

        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        UUID.randomUUID(),
                        "url"
                )
        );

        assertEquals("User not found", ex.getMessage());
    }

    /* ===================== Helper ===================== */

    private ActivityRegistration mockRegistration(
            UUID activityId,
            Long userId,
            UUID registrationId
    ) {

        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Hackathon");

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(registrationId);
        registration.setUser(user);
        registration.setActivity(activity);

        return registration;
    }
}
