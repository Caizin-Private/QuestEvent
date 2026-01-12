package com.questevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramRegistrationResponseDTO {
    private UUID programRegistrationId;
    private UUID programId;
    private String programTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private Instant registeredAt;
    private String message;
}