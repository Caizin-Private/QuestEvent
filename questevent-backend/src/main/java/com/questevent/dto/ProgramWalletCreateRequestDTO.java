package com.questevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramWalletCreateRequestDTO {

    private Long userId;
    private UUID programId;
}
