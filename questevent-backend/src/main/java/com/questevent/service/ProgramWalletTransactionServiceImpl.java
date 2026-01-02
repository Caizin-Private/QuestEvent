package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgramWalletTransactionServiceImpl implements ProgramWalletTransactionService {

    private final ProgramWalletRepository programWalletRepository;
    private final ProgramRepository programRepository;
    private final UserWalletRepository userWalletRepository;

    public ProgramWalletTransactionServiceImpl(ProgramWalletRepository programWalletRepository, ProgramRepository programRepository, UserWalletRepository userWalletRepository) {
        this.programWalletRepository = programWalletRepository;
        this.programRepository = programRepository;
        this.userWalletRepository = userWalletRepository;
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

    @Transactional
    @Override
    public void autoSettleExpiredProgramWallets() {

        List<Program> completedPrograms =
                programRepository.findByStatusAndEndDateBefore(
                        ProgramStatus.COMPLETED,
                        LocalDateTime.now()
                );

        for (Program program : completedPrograms) {
            List<ProgramWallet> wallets =
                    programWalletRepository
                            .findByProgramProgramId(program.getProgramId());
            for (ProgramWallet programWallet : wallets) {
                if (programWallet.getGems() == 0) {
                    continue;
                }
                UserWallet userWallet = userWalletRepository
                                .findByUserUserId(programWallet.getUser().getUserId())
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "User wallet not found for userId "
                                                        + programWallet.getUser().getUserId()
                                        ));
                userWallet.setGems(
                        userWallet.getGems() + programWallet.getGems()
                );
                programWallet.setGems(0);
            }

            program.setStatus(ProgramStatus.SETTLED);
        }
    }

    @Transactional
    @Override
    public void manuallySettleExpiredProgramWallets(Long programId) {

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (program.getStatus() == ProgramStatus.SETTLED) {
            throw new IllegalStateException("Program already settled");
        }

        List<ProgramWallet> wallets =
                programWalletRepository.findByProgramProgramId(programId);

        for (ProgramWallet programWallet : wallets) {

            UserWallet userWallet = programWallet.getUser().getWallet();
            userWallet.setGems(userWallet.getGems() + programWallet.getGems());
            programWallet.setGems(0);

        }

        program.setStatus(ProgramStatus.SETTLED);
    }
}
