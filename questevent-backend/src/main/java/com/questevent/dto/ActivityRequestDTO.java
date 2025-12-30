package com.questevent.dto;

import lombok.Data;

@Data
public class ActivityRequestDTO {
    private String activityName;
    private Integer activityDuration;
    private String activityRulebook;
    private String activityDescription;
    private Integer rewardGems;
    private Boolean isCompulsory;
}

