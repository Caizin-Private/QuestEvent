package com.questevent.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserWalletBalanceDTO {
    private UUID walletId;
    private Long gems;
}
