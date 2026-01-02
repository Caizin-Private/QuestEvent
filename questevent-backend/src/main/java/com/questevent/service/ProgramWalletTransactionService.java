package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.User;
import jakarta.transaction.Transactional;

public interface ProgramWalletTransactionService {

    void creditGems(User user, Program program, int amount);

    void autoSettleExpiredProgramWallets();

    void manuallySettleExpiredProgramWallets(Long programId);
}
