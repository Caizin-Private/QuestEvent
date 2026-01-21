package com.questevent.dto;

import com.questevent.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ActivityWithRegistrationStatusDTO {

    private UUID activityId;
    private String activityName;
    private Boolean isRegistered;
    private CompletionStatus completionStatus;

}
