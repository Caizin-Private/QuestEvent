package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AddParticipantInProgramRequestDTO {
    private Long userId;
    private UUID programId;

}
