package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
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

        // 1️⃣ Eligibility check
        boolean isRegistered = registrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId);

        if (!isRegistered) {
            throw new RuntimeException("User is not registered for this activity");
        }

        // 2️⃣ Get registration ID safely
        ActivityRegistration registration = registrationRepository
                .findAll()
                .stream()
                .filter(r ->
                        r.getActivity().getActivityId().equals(activityId) &&
                                r.getUser().getUserId().equals(userId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Registration record not found"));

        // 3️⃣ Prevent duplicate submission
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

        submissionRepository.save(submission);
    }
}
