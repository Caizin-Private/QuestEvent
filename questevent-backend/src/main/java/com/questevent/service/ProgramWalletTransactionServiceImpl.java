package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramWalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProgramWalletTransactionServiceImpl implements ProgramWalletTransactionService {

    private final ProgramWalletRepository programWalletRepository;

    public ProgramWalletTransactionServiceImpl(ProgramWalletRepository programWalletRepository) {
        this.programWalletRepository = programWalletRepository;
    }

    @Override
    @Transactional
    public void creditGems(User user, Program program, int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        ProgramWallet wallet = programWalletRepository
                .findByUserUserIdAndProgramProgramId(
                        user.getUserId(),
                        program.getProgramId()
                )
                .orElseThrow(() ->
                        new RuntimeException("Program wallet not found"));

        wallet.setGems(wallet.getGems() + amount);
        programWalletRepository.save(wallet);
    }
}
