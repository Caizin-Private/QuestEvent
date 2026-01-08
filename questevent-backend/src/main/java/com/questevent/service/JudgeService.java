package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;

import java.util.List;
import java.util.UUID;

public interface JudgeService {

    List<JudgeSubmissionDTO> getSubmissionsForActivity(UUID activityId);
    List<JudgeSubmissionDTO> getPendingSubmissions();
    void reviewSubmission(UUID submissionId ); //
}
