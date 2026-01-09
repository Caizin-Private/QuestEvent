package com.questevent.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserWalletBalanceDTO {
    private UUID walletId;
    private Long gems;
    private Instant createdAt;
    private Instant updatedAt;
}
