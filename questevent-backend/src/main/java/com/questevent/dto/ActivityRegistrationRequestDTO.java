package com.questevent.dto;

import com.questevent.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRegistrationRequestDTO {
    private Long activityId;
    private Long userId;
    private CompletionStatus completionStatus;
}