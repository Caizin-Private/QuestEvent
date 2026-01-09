package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void submitActivity(Long activityId, String submissionUrl) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new RuntimeException("Unauthorized submission attempt");
        }

        User hostUser = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.debug(
                "Submit activity requested | activityId={} | userId={}",
                activityId,
                hostUser.getUserId()
        );

        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(
                        activityId,
                        hostUser.getUserId()
                )
                .orElseThrow(() -> {
                    log.error(
                            "User not registered for activity | activityId={} | userId={}",
                            activityId,
                            hostUser.getUserId()
                    );
                    return new RuntimeException("User is not registered for this activity");
                });

        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            log.warn(
                    "Resubmission attempt blocked | activityId={} | userId={} | registrationId={}",
                    activityId,
                    hostUser.getUserId(),
                    registration.getActivityRegistrationId()
            );
            throw new RuntimeException("Activity already completed. Submission not allowed.");
        }

        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );

        if (alreadySubmitted) {
            log.warn(
                    "Duplicate submission detected | activityId={} | userId={} | registrationId={}",
                    activityId,
                    hostUser.getUserId(),
                    registration.getActivityRegistrationId()
            );
            throw new RuntimeException("Submission already exists for this registration");
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
                hostUser.getUserId(),
                registration.getActivityRegistrationId(),
                submission.getSubmissionId()
        );
    }
}
