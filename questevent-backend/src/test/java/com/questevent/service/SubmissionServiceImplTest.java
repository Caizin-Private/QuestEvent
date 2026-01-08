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

    /* =====================================================
       SUCCESS CASE
       ===================================================== */

    @Test
    void submitActivity_shouldSaveSubmissionSuccessfully() {

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        // ✅ FIXED
        when(submissionRepository
                .existsByActivityRegistration(registration))
                .thenReturn(false);

        submissionService.submitActivity(
                1L,
                2L,
                "https://github.com/project"
        );

        verify(submissionRepository).save(any(ActivitySubmission.class));
        assertEquals(
                CompletionStatus.COMPLETED,
                registration.getCompletionStatus()
        );
    }

    /* =====================================================
       FAILURE CASES
       ===================================================== */

    @Test
    void submitActivity_shouldThrowIfUserNotRegistered() {

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        1L,
                        2L,
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

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        1L,
                        2L,
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

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        // ✅ FIXED
        when(submissionRepository
                .existsByActivityRegistration(registration))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(
                        1L,
                        2L,
                        "url"
                )
        );

        assertEquals(
                "Submission already exists for this registration",
                ex.getMessage()
        );
    }

    /* =====================================================
       TEST DATA
       ===================================================== */

    private ActivityRegistration mockRegistration() {

        User user = new User();
        user.setUserId(2L);
        user.setName("Test User");

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Hackathon");

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(1L);
        registration.setUser(user);
        registration.setActivity(activity);

        return registration;
    }
}
