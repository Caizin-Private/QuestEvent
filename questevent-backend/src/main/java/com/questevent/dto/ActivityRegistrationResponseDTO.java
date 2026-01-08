package com.questevent.dto;

import com.questevent.enums.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRegistrationResponseDTO {
    private Long activityRegistrationId;
    private Long activityId;
    private String activityName;
    private Long userId;
    private String userName;
    private String userEmail;
    private CompletionStatus completionStatus;
    private Long rewardGems;
    private String message;
}