package com.questevent.dto;

import jakarta.validation.constraints.NotBlank;

public record ResubmitActivityRequestDTO(
        @NotBlank(message = "Submission URL must not be empty")
        String submissionUrl
) {}
