package com.questevent.service;

import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
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


        ActivityRegistration registration = registrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId)
                .orElseThrow(() -> new RuntimeException("User is not registered"));

        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl(submissionUrl);

        submissionRepository.save(submission);
    }
}