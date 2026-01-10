package com.questevent.dto;

import jakarta.validation.constraints.NotBlank;

public record JudgeRejectionRequestDTO(
        @NotBlank(message = "Rejection reason must not be empty")
        String rejectionReason
) {}
