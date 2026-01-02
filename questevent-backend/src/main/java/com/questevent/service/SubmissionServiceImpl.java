package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;

    @Override
    public void submitActivity(Long activityId, Long userId, String submissionUrl) {


        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId)
                .orElseThrow(() ->
                        new RuntimeException("User is not registered for this activity"));

        if (registration.getCompletionStatus() == CompletionStatus.COMPLETED) {
            throw new RuntimeException("Activity already completed. Submission not allowed.");
        }



        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );

        if (alreadySubmitted) {
            throw new RuntimeException("Submission already exists for this registration");
        }


        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl(submissionUrl);

        submissionRepository.save(submission);
    }
}
