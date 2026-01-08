package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.User;

import java.util.UUID;

public interface ProgramWalletTransactionService {

    void creditGems(User user, Program program, Long amount);

    void autoSettleExpiredProgramWallets();

    void manuallySettleExpiredProgramWallets(UUID programId);
}
