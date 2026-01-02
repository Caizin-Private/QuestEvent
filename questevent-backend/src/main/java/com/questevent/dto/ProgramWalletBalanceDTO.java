package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProgramWalletBalanceDTO {

    private UUID programWalletId;
    private int gems;
}
