package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.exception.InvalidOperationException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void submitActivity(UUID activityId, String submissionUrl) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {

            log.warn("Authenticated user not found while submitting activity");
            throw new ResourceNotFoundException("User not found");
        }

        Long userId = principal.userId();

        log.debug(
                "Submit activity requested | activityId={} | userId={}",
                activityId,
                userId
        );

        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId)
                .orElseThrow(() -> {
                    log.error(
                            "User not registered for activity | activityId={} | userId={}",
                            activityId,
                            userId
                    );
                    return new ResourceNotFoundException(
                            "User is not registered for this activity"
                    );
                });

        ActivitySubmission existingSubmission =
                submissionRepository
                        .findByActivityRegistration_ActivityRegistrationId(
                                registration.getActivityRegistrationId()
                        )
                        .orElse(null);


        if (existingSubmission != null &&
                existingSubmission.getReviewStatus() == ReviewStatus.APPROVED) {

            log.warn(
                    "Resubmission blocked (already approved) | activityId={} | userId={}",
                    activityId,
                    userId
            );

            throw new InvalidOperationException(
                    "Submission already approved. Resubmission not allowed."
            );
        }

        if (existingSubmission != null) {

            log.info(
                    "Resubmitting activity | activityId={} | userId={} | submissionId={}",
                    activityId,
                    userId,
                    existingSubmission.getSubmissionId()
            );

            existingSubmission.setSubmissionUrl(submissionUrl);
            existingSubmission.setReviewStatus(ReviewStatus.PENDING);
            existingSubmission.setReviewedAt(null);

            registration.setCompletionStatus(CompletionStatus.COMPLETED);

            submissionRepository.save(existingSubmission);
            registrationRepository.save(registration);
            return;
        }

        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl(submissionUrl);

        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        submissionRepository.save(submission);
        registrationRepository.save(registration);

        log.info(
                "Activity submitted successfully | activityId={} | userId={} | registrationId={} | submissionId={}",
                activityId,
                userId,
                registration.getActivityRegistrationId(),
                submission.getSubmissionId()
        );
    }


    @Override
    @Transactional
    public void resubmitActivity(UUID activityId, String submissionUrl, Authentication authentication) {
        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        Long userId = principal.userId();

        log.debug(
                "Resubmit activity requested | activityId={} | userId={}",
                activityId,
                userId
        );

        ActivityRegistration registration =
                registrationRepository
                        .findByActivityActivityIdAndUserUserId(
                                activityId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User is not registered for this activity"
                                )
                        );

        ActivitySubmission submission =
                submissionRepository
                        .findByActivityRegistration_ActivityRegistrationId(
                                registration.getActivityRegistrationId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Submission not found"
                                )
                        );

        if (submission.getReviewStatus() != ReviewStatus.REJECTED) {
            throw new InvalidOperationException(
                    "Only rejected submissions can be resubmitted"
            );
        }


        submission.setSubmissionUrl(submissionUrl);
        submission.setReviewStatus(ReviewStatus.PENDING);
        submission.setReviewedAt(null);


        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        submissionRepository.save(submission);
        registrationRepository.save(registration);

        log.info(
                "Activity resubmitted successfully | activityId={} | userId={} | submissionId={}",
                activityId,
                userId,
                submission.getSubmissionId()
        );
    }


}
