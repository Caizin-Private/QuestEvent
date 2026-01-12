package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.exception.ActivityAlreadyCompletedException;
import com.questevent.exception.SubmissionAlreadyExistsException;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void submitActivity(UUID activityId, Long userId, String submissionUrl) {

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
                    return new RuntimeException("User is not registered for this activity");
                });

        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            log.warn(
                    "Resubmission attempt blocked | activityId={} | userId={} | registrationId={}",
                    activityId,
                    userId,
                    registration.getActivityRegistrationId()
            );
            throw new ActivityAlreadyCompletedException(
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
            throw new SubmissionAlreadyExistsException(
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
