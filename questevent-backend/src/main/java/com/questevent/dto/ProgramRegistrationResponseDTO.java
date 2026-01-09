package com.questevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramRegistrationResponseDTO {
    private Long programRegistrationId;
    private Long programId;
    private String programTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private Instant registeredAt;
    private String message;
}