package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
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

        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            log.warn(
                    "Resubmission attempt blocked | activityId={} | userId={} | registrationId={}",
                    activityId,
                    userId,
                    registration.getActivityRegistrationId()
            );
            throw new InvalidOperationException(
                    "Activity already completed. Submission not allowed."
            );
        }

        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistrationActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );

        if (alreadySubmitted) {
            log.warn(
                    "Duplicate submission detected | activityId={} | userId={} | registrationId={}",
                    activityId,
                    userId,
                    registration.getActivityRegistrationId()
            );
            throw new ResourceConflictException(
                    "Submission already exists for this registration"
            );
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

}
