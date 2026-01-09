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
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private static final String UNAUTHORIZED_MSG =
            "Unauthorized submission attempt";
    private static final String USER_NOT_FOUND_MSG =
            "User not found";
    private static final String NOT_REGISTERED_MSG =
            "User is not registered for this activity";
    private static final String ALREADY_COMPLETED_MSG =
            "Activity already completed. Submission not allowed.";
    private static final String DUPLICATE_SUBMISSION_MSG =
            "Submission already exists for this registration";

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void submitActivity(Long activityId, String submissionUrl) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(UNAUTHORIZED, UNAUTHORIZED_MSG);
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        USER_NOT_FOUND_MSG
                ));

        log.debug(
                "Submit activity requested | activityId={} | userId={}",
                activityId,
                user.getUserId()
        );

        ActivityRegistration registration =
                registrationRepository
                        .findByActivityActivityIdAndUserUserId(
                                activityId,
                                user.getUserId()
                        )
                        .orElseThrow(() -> {
                            log.error(
                                    "User not registered for activity | activityId={} | userId={}",
                                    activityId,
                                    user.getUserId()
                            );
                            return new ResponseStatusException(
                                    BAD_REQUEST,
                                    NOT_REGISTERED_MSG
                            );
                        });

        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            log.warn(
                    "Resubmission attempt blocked | activityId={} | userId={} | registrationId={}",
                    activityId,
                    user.getUserId(),
                    registration.getActivityRegistrationId()
            );
            throw new ResponseStatusException(
                    CONFLICT,
                    ALREADY_COMPLETED_MSG
            );
        }

        boolean alreadySubmitted =
                submissionRepository
                        .existsByActivityRegistration_ActivityRegistrationId(
                                registration.getActivityRegistrationId()
                        );

        if (alreadySubmitted) {
            log.warn(
                    "Duplicate submission detected | activityId={} | userId={} | registrationId={}",
                    activityId,
                    user.getUserId(),
                    registration.getActivityRegistrationId()
            );
            throw new ResponseStatusException(
                    CONFLICT,
                    DUPLICATE_SUBMISSION_MSG
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
                user.getUserId(),
                registration.getActivityRegistrationId(),
                submission.getSubmissionId()
        );
    }
}
