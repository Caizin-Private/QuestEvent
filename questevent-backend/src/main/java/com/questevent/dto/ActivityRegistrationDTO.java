package com.questevent.dto;

import com.questevent.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRegistrationDTO {
    private UUID activityRegistrationId;
    private UUID activityId;
    private String activityName;
    private UUID userId;
    private String userName;
    private CompletionStatus completionStatus;
}