package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.Instant;

public record JudgeSubmissionDTO(
        Long submissionId,
        Long activityId,
        String activityName,
        Long userId,
        String userName,
        String submissionUrl,
        Integer awardedGems,
        Instant submittedAt,
        Instant reviewedAt,
        ReviewStatus reviewStatus
) {
}
