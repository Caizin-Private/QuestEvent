package com.questevent.service;

import com.questevent.entity.Activity;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityRegistrationRepository registrationRepository;

    @Override
    public void submitActivity(Long activityId, Long userId, String submissionUrl) {

        // 1️⃣ Eligibility check
        boolean registered = registrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId);

        if (!registered) {
            throw new RuntimeException("User is not registered for this activity");
        }

        // 2️⃣ Load entities
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3️⃣ Create submission
        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivity(activity);
        submission.setUser(user);
        submission.setSubmissionUrl(submissionUrl);

        submissionRepository.save(submission);
    }
}