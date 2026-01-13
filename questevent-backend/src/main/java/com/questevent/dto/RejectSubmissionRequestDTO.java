package com.questevent.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectSubmissionRequestDTO(

        @NotBlank(message = "Rejection reason must not be empty")
        String reason

) {}
