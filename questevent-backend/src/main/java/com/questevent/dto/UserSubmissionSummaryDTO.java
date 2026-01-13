package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record UserSubmissionSummaryDTO(
        UUID submissionId,
        UUID activityId,
        String activityTitle,
        ReviewStatus reviewStatus,
        Instant submittedAt,
        Instant reviewedAt
) {}
