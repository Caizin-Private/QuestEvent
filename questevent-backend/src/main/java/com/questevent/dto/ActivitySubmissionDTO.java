package com.questevent.dto;

import java.time.Instant;


public record ActivitySubmissionDTO(
        Long submissionId,
        Long activityId,
        Long userId,
        String submissionUrl,
        Integer awardedGems,
        Instant submittedAt,
        Instant reviewedAt
) {}