package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record JudgeSubmissionDetailsDTO(
        UUID submissionId,
        UUID activityId,
        String activityTitle,
        Long userId,
        String userEmail,
        String submissionUrl,
        ReviewStatus reviewStatus,
        String rejectionReason,
        Instant reviewedAt
) {}
