package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface JudgeService {

    List<JudgeSubmissionDTO> getAllSubmissionsForJudge(Authentication authentication);

    List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(Authentication authentication);

    List<JudgeSubmissionDTO> getSubmissionsForActivity(Long activityId);

    void reviewSubmission(Long submissionId);
}
