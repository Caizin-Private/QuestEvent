package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.exception.InvalidOperationException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.utils.SecurityUserResolver;
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

    @Mock
    private SecurityUserResolver securityUserResolver; // ✅ REQUIRED

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private static final Long USER_ID = 1L;

    private void mockAuthenticatedUser() {
        when(securityUserResolver.getCurrentUserPrincipal())
                .thenReturn(
                        new UserPrincipal(
                                USER_ID,
                                "test@example.com",
                                Role.USER
                        )
                );
    }

    @Test
    void submitActivity_shouldCreateNewSubmissionSuccessfully() {

        mockAuthenticatedUser();

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(registrationId))
                .thenReturn(Optional.empty());

        submissionService.submitActivity(activityId, "https://github.com/project");

        verify(submissionRepository).save(any(ActivitySubmission.class));
        verify(registrationRepository).save(registration);

        assertEquals(
                CompletionStatus.COMPLETED,
                registration.getCompletionStatus()
        );
    }

    @Test
    void submitActivity_shouldResubmitIfSubmissionExistsAndNotApproved() {

        mockAuthenticatedUser();

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);

        ActivitySubmission existingSubmission = new ActivitySubmission();
        existingSubmission.setReviewStatus(ReviewStatus.REJECTED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(registrationId))
                .thenReturn(Optional.of(existingSubmission));

        submissionService.submitActivity(activityId, "new-url");

        assertEquals(
                ReviewStatus.PENDING,
                existingSubmission.getReviewStatus()
        );

        verify(submissionRepository).save(existingSubmission);
        verify(registrationRepository).save(registration);
    }

    @Test
    void submitActivity_shouldThrowIfSubmissionAlreadyApproved() {

        mockAuthenticatedUser();

        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        ActivityRegistration registration =
                mockRegistration(activityId, USER_ID, registrationId);

        ActivitySubmission approvedSubmission = new ActivitySubmission();
        approvedSubmission.setReviewStatus(ReviewStatus.APPROVED);

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, USER_ID))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(registrationId))
                .thenReturn(Optional.of(approvedSubmission));

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> submissionService.submitActivity(activityId, "url")
        );

        assertEquals(
                "Submission already approved. Resubmission not allowed.",
                ex.getMessage()
        );

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitActivity_shouldThrowIfUserNotRegistered() {

        mockAuthenticatedUser();

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(any(), any()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> submissionService.submitActivity(
                        UUID.randomUUID(),
                        "url"
                )
        );

        assertEquals(
                "User is not registered for this activity",
                ex.getMessage()
        );
    }

    private ActivityRegistration mockRegistration(
            UUID activityId,
            Long userId,
            UUID registrationId
    ) {

        User user = new User();
        user.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(registrationId);
        registration.setUser(user);
        registration.setActivity(activity);

        return registration;
    }
}
