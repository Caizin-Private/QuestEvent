package com.questevent.dto;

import com.questevent.enums.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDetailsResponseDTO(
        UUID submissionId,
        UUID activityId,
        String submissionUrl,
        ReviewStatus reviewStatus,
        Long awardedGems,
        Instant reviewedAt
) {}
