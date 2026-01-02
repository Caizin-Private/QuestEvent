package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.LocalDateTime;

public record JudgeSubmissionDTO(
        Long submissionId,
        Long activityId,
        String activityName,
        Long userId,
        String userName,
        String submissionUrl,
        Integer awardedGems,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        ReviewStatus reviewStatus
) {
}
