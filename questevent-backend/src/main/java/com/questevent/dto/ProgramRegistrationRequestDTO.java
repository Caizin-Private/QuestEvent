package com.questevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramRegistrationRequestDTO {
    private Long programId;
    private Long userId;
}