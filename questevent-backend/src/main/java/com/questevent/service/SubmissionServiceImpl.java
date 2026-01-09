package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void submitActivity(UUID activityId, Long userId, String submissionUrl) {


        // 1️⃣ Validate registration
        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId)
                .orElseThrow(() ->
                        new RuntimeException("User is not registered for this activity"));

        // 2️⃣ Prevent resubmission
        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new RuntimeException("Activity already completed. Submission not allowed.");
        }

        // 3️⃣ Extra safety check (service-level)
        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );

        if (alreadySubmitted) {
            throw new RuntimeException("Submission already exists for this registration");
        }

        // 4️⃣ Create submission
        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl(submissionUrl);
        // 5️⃣ Mark registration as completed
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        // 6️⃣ Persist (transaction ensures atomicity)
        submissionRepository.save(submission);
        registrationRepository.save(registration);
    }
}
