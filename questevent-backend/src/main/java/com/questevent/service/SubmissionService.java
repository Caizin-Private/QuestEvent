package com.questevent.service;

import java.util.UUID;

public interface SubmissionService {

    void submitActivity(
            UUID activityId,
            String submissionUrl
    );
}