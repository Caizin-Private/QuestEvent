package com.questevent.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivitySubmissionDTO(
        UUID submissionId,
        UUID activityId,
        Long userId,
        String submissionUrl,
        Long awardedGems,
        Instant submittedAt,
        Instant reviewedAt
) {}