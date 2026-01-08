package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProgramWalletBalanceDTO {

    private UUID programWalletId;
    private UUID programId;
    private UUID userId;
    private Long gems;
}
