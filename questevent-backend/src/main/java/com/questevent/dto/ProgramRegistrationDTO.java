package com.questevent.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramRegistrationDTO {
    private Long programRegistrationId;
    private Long programId;
    private String programTitle;
    private Long userId;
    private String userName;
    private Instant registeredAt;
}



