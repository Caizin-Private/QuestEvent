package com.questevent.dto;

import com.questevent.entity.Program;
import com.questevent.entity.User;
import lombok.Data;

import java.util.UUID;

@Data
public class ProgramWalletBalanceDto {

    private UUID programWalletId;
    private int gems;
}
