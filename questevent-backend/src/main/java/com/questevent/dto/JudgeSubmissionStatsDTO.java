package com.questevent.dto;

public record JudgeSubmissionStatsDTO(
        long pending,
        long approved,
        long rejected
) {}
