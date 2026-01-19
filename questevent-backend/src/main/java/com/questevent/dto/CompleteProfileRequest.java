package com.questevent.dto;

import com.questevent.enums.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteProfileRequest(
        @NotNull Department department,
        @NotBlank String gender
        ) {
}
