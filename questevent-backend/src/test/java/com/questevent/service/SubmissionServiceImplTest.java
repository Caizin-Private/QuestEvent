package com.questevent.service;

import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void submitActivity_shouldSaveSubmissionSuccessfully() {

        UUID activityId = UUID.randomUUID();
        Long userId = 1L;
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, userId, registrationId);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistrationActivityRegistrationId(registrationId))
                .thenReturn(false);

        submissionService.submitActivity(
                activityId,
                userId,
                "https://github.com/project"
        );

        verify(submissionRepository).save(any(ActivitySubmission.class));
        assertEquals(
                CompletionStatus.COMPLETED,
                registration.getCompletionStatus()
        );
    }

    @Test
    void submitActivity_shouldThrowIfUserNotRegistered() {

        UUID activityId = UUID.randomUUID();
        Long userId = 1L;

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        userId,
                        "url"
                )
        );

        assertEquals(
                "User is not registered for this activity",
                ex.getMessage()
        );
    }

    @Test
    void submitActivity_shouldThrowIfActivityAlreadyCompleted() {

        UUID activityId = UUID.randomUUID();
        Long userId = 1L;
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, userId, registrationId);
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.of(registration));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        userId,
                        "url"
                )
        );

        assertEquals(
                "Activity already completed. Submission not allowed.",
                ex.getMessage()
        );
    }

    @Test
    void submitActivity_shouldThrowIfSubmissionAlreadyExists() {

        UUID activityId = UUID.randomUUID();
        Long userId = 1L;
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, userId, registrationId);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistrationActivityRegistrationId(registrationId))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        activityId,
                        userId,
                        "url"
                )
        );

        assertEquals(
                "Submission already exists for this registration",
                ex.getMessage()
        );
    }

    /* ===================== helper ===================== */

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
