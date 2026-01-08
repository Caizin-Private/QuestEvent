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
public class ProgramWalletTransactionServiceImpl
        implements ProgramWalletTransactionService {

    private final ProgramWalletRepository programWalletRepository;
    private final ProgramRepository programRepository;
    private final UserWalletRepository userWalletRepository;

    public ProgramWalletTransactionServiceImpl(
            ProgramWalletRepository programWalletRepository,
            ProgramRepository programRepository,
            UserWalletRepository userWalletRepository) {
        this.programWalletRepository = programWalletRepository;
        this.programRepository = programRepository;
        this.userWalletRepository = userWalletRepository;
    }

    @Override
    @Transactional
    public void creditGems(User user, Program program, Long amount) {

        if (user == null || user.getUserId() == null ||
                program == null || program.getProgramId() == null) {
            throw new IllegalArgumentException("Invalid user or program");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        ProgramWallet wallet = programWalletRepository
                .findByUserUserIdAndProgramProgramId(
                        user.getUserId(),
                        program.getProgramId()
                )
                .orElseThrow(() ->
                        new IllegalStateException("Program wallet not found")
                );

        wallet.setGems(wallet.getGems() + amount);
        programWalletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void autoSettleExpiredProgramWallets() {

        List<Program> expiredPrograms =
                programRepository.findByStatusAndEndDateBefore(
                        ProgramStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (Program program : expiredPrograms) {

            List<ProgramWallet> wallets =
                    programWalletRepository
                            .findByProgramProgramId(program.getProgramId());

            for (ProgramWallet programWallet : wallets) {

                Long gems = programWallet.getGems();
                if (gems <= 0) {
                    continue;
                }

                UserWallet userWallet = userWalletRepository
                        .findByUserUserId(
                                programWallet.getUser().getUserId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException("User wallet not found")
                        );

                userWallet.setGems(userWallet.getGems() + gems);
                programWallet.setGems(0L);

                userWalletRepository.save(userWallet);
                programWalletRepository.save(programWallet);
            }

            program.setStatus(ProgramStatus.COMPLETED);
            programRepository.save(program);
        }
    }


    @Override
    @Transactional
    public void manuallySettleExpiredProgramWallets(Long programId) {

        if (programId == null || programId <= 0) {
            throw new IllegalArgumentException("Invalid programId");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new IllegalStateException("Program not found")
                );

        if (program.getStatus() == ProgramStatus.COMPLETED) {
            throw new IllegalStateException("Program already completed");
        }

        List<ProgramWallet> wallets =
                programWalletRepository.findByProgramProgramId(programId);

        for (ProgramWallet programWallet : wallets) {

            Long gems = programWallet.getGems();
            if (gems <= 0) {
                continue;
            }

            UserWallet userWallet = programWallet.getUser().getWallet();
            if (userWallet == null) {
                throw new IllegalStateException("User wallet not found");
            }

            userWallet.setGems(userWallet.getGems() + gems);
            programWallet.setGems(0L);

            userWalletRepository.save(userWallet);
            programWalletRepository.save(programWallet);
        }

        program.setStatus(ProgramStatus.COMPLETED);
        programRepository.save(program);
    }
}
