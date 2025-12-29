package com.questevent.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class WalletBalanceDto {
    private UUID walletId;
    private int gems;
}
