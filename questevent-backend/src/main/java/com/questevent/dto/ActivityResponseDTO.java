package com.questevent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityResponseDTO {
    private Long activityId;
    private Long programId;
    private String activityName;
    private Integer activityDuration;
    private String activityRulebook;
    private String activityDescription;
    private Integer rewardGems;
    private LocalDateTime createdAt;
    private Boolean isCompulsory;
}