package com.questevent.service;

public interface SubmissionService {

    void submitActivity(
            Long activityId,
            String submissionUrl
    );
}