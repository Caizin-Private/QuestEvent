package com.questevent.scheduler;

import com.questevent.service.ProgramWalletTransactionService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ProgramWalletScheduler {

    private final ProgramWalletTransactionService transactionService;

    public ProgramWalletScheduler(
            ProgramWalletTransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    // Runs daily at 12:01 AM to automatically settle program wallets
    // for programs whose endDate has passed
    @Scheduled(cron = "0 1 0 * * ?")
    public void autoSettleProgramWallets() {
        transactionService.autoSettleExpiredProgramWallets();
    }
}
