package com.questevent.dto;

import lombok.Data;

@Data
public class AddParticipantInProgramRequestDTO {
    private Long userId;
    private Long programId;

}
