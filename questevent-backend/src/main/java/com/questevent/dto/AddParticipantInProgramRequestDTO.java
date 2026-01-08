package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddParticipantInProgramRequestDTO {
    private UUID userId;
    private UUID programId;

}
