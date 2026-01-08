package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddParticipantInActivityRequestDTO {
    private UUID userId;
    private UUID activityId;
}
