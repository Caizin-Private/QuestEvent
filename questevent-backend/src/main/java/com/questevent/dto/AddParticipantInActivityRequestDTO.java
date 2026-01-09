package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddParticipantInActivityRequestDTO {
    private Long userId;
    private UUID activityId;
}
