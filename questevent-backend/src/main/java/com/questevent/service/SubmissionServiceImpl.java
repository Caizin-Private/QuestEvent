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
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ActivityRegistrationRepository registrationRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void submitActivity(Long activityId, String submissionUrl) {
        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User hostUser = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                hostUser = userRepository.findById(p.userId()).orElse(null);
            }
        }


        // 1️⃣ Validate registration
        assert hostUser != null;
        ActivityRegistration registration = registrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, hostUser.getUserId())
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
