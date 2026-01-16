package com.questevent.dto;

import com.questevent.enums.Department;

public record UserUpdateRequest(
        Department department,
        String gender
) {
}
