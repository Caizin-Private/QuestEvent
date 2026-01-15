package com.questevent.dto;

import com.questevent.enums.ReviewStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public class SubmissionStatusResponseDTO {
    private UUID submissionId;
    private ReviewStatus reviewStatus;
    private Instant submittedAt;
    private Instant reviewedAt;
    private Long awardedGems;
}
