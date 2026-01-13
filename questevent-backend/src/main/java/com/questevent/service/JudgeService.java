package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.JudgeSubmissionDetailsDTO;
import com.questevent.dto.JudgeSubmissionStatsDTO;
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

    void rejectSubmission(UUID submissionId, String reason, Authentication authentication);

    void reviewSubmission(UUID submissionId);

    JudgeSubmissionDetailsDTO getSubmissionDetails(UUID submissionId, Authentication authentication);

    JudgeSubmissionStatsDTO getSubmissionStats(Authentication authentication);

}
