package com.questevent.service;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface SubmissionService {

    void submitActivity(
            UUID activityId,
            String submissionUrl
    );

    void resubmitActivity(
            UUID activityId,
            String submissionUrl,
            Authentication authentication
    );

}