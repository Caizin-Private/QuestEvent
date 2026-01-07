package com.questevent.dto;

import lombok.Data;

@Data
public class AddParticipantInActivityRequestDTO {
    private Long userId;
    private Long activityId;
}
