package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.exception.InvalidOperationException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private SubmissionServiceImpl service;

    private User user;
    private ActivityRegistration registration;
    private ActivitySubmission submission;
    private UUID activityId;

    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();

        user = new User();
        user.setUserId(1L);

        registration = new ActivityRegistration();
        registration.setActivityRegistrationId(UUID.randomUUID());
        registration.setUser(user);

        submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setReviewStatus(ReviewStatus.PENDING);
    }

    @Test
    void submitActivity_success_newSubmission() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()))
                .thenReturn(Optional.empty());

        service.submitActivity(activityId, "url");

        verify(submissionRepository).save(any(ActivitySubmission.class));
        verify(registrationRepository).save(registration);
        assertThat(registration.getCompletionStatus())
                .isEqualTo(CompletionStatus.COMPLETED);
    }

    @Test
    void submitActivity_success_existingPendingSubmission() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()))
                .thenReturn(Optional.of(submission));

        service.submitActivity(activityId, "new-url");

        assertThat(submission.getSubmissionUrl()).isEqualTo("new-url");
        assertThat(submission.getReviewStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(submissionRepository).save(submission);
        verify(registrationRepository).save(registration);
    }

    @Test
    void submitActivity_fails_whenAlreadyApproved() {
        submission.setReviewStatus(ReviewStatus.APPROVED);

        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()))
                .thenReturn(Optional.of(submission));

        assertThatThrownBy(() ->
                service.submitActivity(activityId, "url"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void submitActivity_fails_whenNotRegistered() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.submitActivity(activityId, "url"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resubmitActivity_success() {
        submission.setReviewStatus(ReviewStatus.REJECTED);

        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()))
                .thenReturn(Optional.of(submission));

        service.resubmitActivity(activityId, "new-url", null);

        assertThat(submission.getSubmissionUrl()).isEqualTo("new-url");
        assertThat(submission.getReviewStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(submissionRepository).save(submission);
        verify(registrationRepository).save(registration);
    }

    @Test
    void resubmitActivity_fails_whenSubmissionNotFound() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.resubmitActivity(activityId, "url", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resubmitActivity_fails_whenNotRejected() {
        submission.setReviewStatus(ReviewStatus.APPROVED);

        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, user.getUserId()))
                .thenReturn(Optional.of(registration));
        when(submissionRepository
                .findByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()))
                .thenReturn(Optional.of(submission));

        assertThatThrownBy(() ->
                service.resubmitActivity(activityId, "url", null))
                .isInstanceOf(InvalidOperationException.class);
    }
}
