package com.questevent.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramRegistrationDTO {
    private UUID programRegistrationId;
    private UUID programId;
    private String programTitle;
    private Long userId;
    private String userName;
    private Instant registeredAt;
}



