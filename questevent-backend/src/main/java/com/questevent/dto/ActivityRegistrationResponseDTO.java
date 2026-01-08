package com.questevent.dto;

import com.questevent.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRegistrationResponseDTO {
    private UUID activityRegistrationId;
    private UUID activityId;
    private String activityName;
    private UUID userId;
    private String userName;
    private String userEmail;
    private CompletionStatus completionStatus;
    private Long rewardGems;
    private String message;
}