package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDto;
import com.questevent.entity.ActivitySubmission;

import java.util.List;

public interface JudgeService {

    List<JudgeSubmissionDto> getSubmissionsForActivity(Long activityId);
    List<JudgeSubmissionDto> getPendingSubmissions();
    void reviewSubmission(Long submissionId, Long judgeId);
}
