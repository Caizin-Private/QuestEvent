package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;

import java.util.List;

public interface JudgeService {

    List<JudgeSubmissionDTO> getSubmissionsForActivity(Long activityId);
    List<JudgeSubmissionDTO> getPendingSubmissions();
    void reviewSubmission(Long submissionId );
}
