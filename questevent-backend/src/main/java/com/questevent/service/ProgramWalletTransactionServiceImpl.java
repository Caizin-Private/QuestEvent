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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
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

        log.debug(
                "Credit gems requested | userId={} | programId={} | amount={}",
                user != null ? user.getUserId() : null,
                program != null ? program.getProgramId() : null,
                amount
        );

        if (user == null || user.getUserId() == null ||
                program == null || program.getProgramId() == null) {
            log.warn("Invalid user or program supplied for program wallet credit");
            throw new IllegalArgumentException("Invalid user or program");
        }

        if (amount <= 0) {
            log.warn(
                    "Invalid credit amount={} for programId={}",
                    amount,
                    program.getProgramId()
            );
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        ProgramWallet wallet = programWalletRepository
                .findByUserUserIdAndProgramProgramId(
                        user.getUserId(),
                        program.getProgramId()
                )
                .orElseThrow(() -> {
                    log.error(
                            "Program wallet not found | userId={} | programId={}",
                            user.getUserId(),
                            program.getProgramId()
                    );
                    return new IllegalStateException("Program wallet not found");
                });

        Long before = wallet.getGems();
        wallet.setGems(before + amount);
        programWalletRepository.save(wallet);

        log.info(
                "Program wallet credited | userId={} | programId={} | before={} | credited={} | after={}",
                user.getUserId(),
                program.getProgramId(),
                before,
                amount,
                wallet.getGems()
        );
    }

    @Override
    @Transactional
    public void autoSettleExpiredProgramWallets() {

        log.info("Auto-settlement job started");

        List<Program> expiredPrograms =
                programRepository.findByStatusAndEndDateBefore(
                        ProgramStatus.ACTIVE,
                        LocalDateTime.now()
                );

        log.debug("Found {} expired active programs", expiredPrograms.size());

        for (Program program : expiredPrograms) {

            log.info(
                    "Auto-settling program | programId={}",
                    program.getProgramId()
            );

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
                        .orElseThrow(() -> {
                            log.error(
                                    "User wallet not found during auto-settlement | userId={}",
                                    programWallet.getUser().getUserId()
                            );
                            return new IllegalStateException("User wallet not found");
                        });

                userWallet.setGems(userWallet.getGems() + gems);
                programWallet.setGems(0L);

                userWalletRepository.save(userWallet);
                programWalletRepository.save(programWallet);

                log.info(
                        "Auto-settled gems | programId={} | userId={} | transferred={} | userBefore={} | userAfter={}",
                        program.getProgramId(),
                        programWallet.getUser().getUserId(),
                        gems,
                        userWallet.getGems()
                );
            }

            program.setStatus(ProgramStatus.COMPLETED);
            programRepository.save(program);

            log.info(
                    "Program marked COMPLETED after auto-settlement | programId={}",
                    program.getProgramId()
            );
        }

        log.info("Auto-settlement job finished");
    }

    @Override
    @Transactional
    public void manuallySettleExpiredProgramWallets(UUID programId) {

        log.debug("Manual settlement requested | programId={}", programId);

        if (programId == null ) {
            log.warn("Invalid programId supplied for manual settlement");
            throw new IllegalArgumentException("Invalid programId");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error("Program not found | programId={}", programId);
                    return new IllegalStateException("Program not found");
                });

        if (program.getStatus() == ProgramStatus.COMPLETED) {
            log.warn("Program already completed | programId={}", programId);
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
                log.error(
                        "User wallet missing during manual settlement | userId={}",
                        programWallet.getUser().getUserId()
                );
                throw new IllegalStateException("User wallet not found");
            }

            userWallet.setGems(userWallet.getGems() + gems);
            programWallet.setGems(0L);

            userWalletRepository.save(userWallet);
            programWalletRepository.save(programWallet);

            log.info(
                    "Manual settlement completed | programId={} | userId={} | transferred={} | userBefore={} | userAfter={}",
                    programId,
                    programWallet.getUser().getUserId(),
                    gems,
                    userWallet.getGems()
            );
        }

        program.setStatus(ProgramStatus.COMPLETED);
        programRepository.save(program);

        log.info(
                "Program marked COMPLETED after manual settlement | programId={}",
                programId
        );
    }
}
