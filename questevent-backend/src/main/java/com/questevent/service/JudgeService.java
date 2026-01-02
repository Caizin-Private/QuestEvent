package com.questevent.service;

import com.questevent.entity.ActivitySubmission;

import java.util.List;

public interface JudgeService {

    List<ActivitySubmission> getSubmissionsForActivity(Long activityId);

    void reviewSubmission(Long submissionId);
}
