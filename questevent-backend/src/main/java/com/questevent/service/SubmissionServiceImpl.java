package com.questevent.service.impl;

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

        // 1️⃣ Check if user is registered for activity

        ActivityRegistration registration = registrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId)
                .orElseThrow(() ->
                        new RuntimeException("User is not registered for this activity"));
        // 2️⃣ Prevent duplicate submission (business rule)

        boolean alreadySubmitted = submissionRepository
                .existsByActivityRegistrationId(registration.getId());
        if (alreadySubmitted) {

            throw new RuntimeException("Submission already exists for this registration");

        }

        // 3️⃣ Create submission

        ActivitySubmission submission = new ActivitySubmission();

        submission.setActivityRegistration(registration);

        submission.setSubmissionUrl(submissionUrl);

        submissionRepository.save(submission);

    }

}

