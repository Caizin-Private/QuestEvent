package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface JudgeService {


    List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication authentication
    );


    List<JudgeSubmissionDTO> getPendingSubmissionsForActivity(
            UUID activityId,
            Authentication authentication
    );


    List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication authentication
    );


    void reviewSubmission(UUID submissionId);
}
