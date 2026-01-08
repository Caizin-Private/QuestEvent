package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface JudgeService {


    List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication authentication
    );


    List<JudgeSubmissionDTO> getPendingSubmissionsForActivity(
            Long activityId,
            Authentication authentication
    );


    List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication authentication
    );


    void reviewSubmission(Long submissionId);
}
