package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record JudgeSubmissionDTO(
        UUID submissionId,
        UUID activityId,
        String activityName,
        UUID userId,
        String userName,
        String submissionUrl,
        Long awardedGems,
        Instant submittedAt,
        Instant reviewedAt,
        ReviewStatus reviewStatus
) {
}
