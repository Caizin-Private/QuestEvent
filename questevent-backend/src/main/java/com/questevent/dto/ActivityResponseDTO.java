package com.questevent.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ActivityResponseDTO {
    private Long activityId;
    private Long programId;
    private String activityName;
    private Integer activityDuration;
    private String activityRulebook;
    private String activityDescription;
    private Integer rewardGems;
    private Instant createdAt;
    private Boolean isCompulsory;
}