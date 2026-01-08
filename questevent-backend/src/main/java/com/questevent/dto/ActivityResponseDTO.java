package com.questevent.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ActivityResponseDTO {
    private UUID activityId;
    private UUID programId;
    private String activityName;
    private Integer activityDuration;
    private String activityRulebook;
    private String activityDescription;
    private Long rewardGems;
    private Instant createdAt;
    private Boolean isCompulsory;
}