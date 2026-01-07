package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void submitActivity(Long activityId, Long userId, String submissionUrl) {

        // 1️⃣ Validate registration
        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User is not registered for this activity")
                );

        // 2️⃣ Prevent resubmission after completion
        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Activity already completed. Submission not allowed."
            );
        }

        // 3️⃣ Prevent duplicate submission (service-level safety)
        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );

        if (alreadySubmitted) {
            throw new IllegalStateException(
                    "Submission already exists for this registration"
            );
        }

        // 4️⃣ Create submission
        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl(submissionUrl);

        // 5️⃣ Mark registration as completed
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        // 6️⃣ Persist atomically
        submissionRepository.save(submission);
        registrationRepository.save(registration);
    }
}

