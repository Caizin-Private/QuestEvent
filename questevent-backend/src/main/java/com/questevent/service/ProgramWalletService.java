package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class ProgramWalletService {

    private final ProgramWalletRepository programWalletRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;

    public ProgramWalletService(
            ProgramWalletRepository programWalletRepository,
            UserRepository userRepository,
            ProgramRepository programRepository
    ) {
        this.programWalletRepository = programWalletRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
    }

    public ProgramWallet createWallet(Long userId, UUID programId) {

        log.debug(
                "Create program wallet requested | userId={} | programId={}",
                userId,
                programId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found while creating program wallet | userId={}", userId);
                    return new RuntimeException("User not found");
                });

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error("Program not found while creating program wallet | programId={}", programId);
                    return new RuntimeException("Program not found");
                });

        if (programWalletRepository.findByUserAndProgram(user, program).isPresent()) {
            log.warn(
                    "Program wallet already exists | userId={} | programId={}",
                    userId,
                    programId
            );
            throw new RuntimeException("ProgramWallet already exists");
        }

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setProgram(program);
        programWallet.setGems(0L);

        ProgramWallet saved = programWalletRepository.save(programWallet);

        log.info(
                "Program wallet created | walletId={} | userId={} | programId={}",
                saved.getProgramWalletId(),
                userId,
                programId
        );

        return saved;
    }

    public ProgramWalletBalanceDTO getWalletBalanceByWalletId(UUID walletId) {

        log.debug("Fetching program wallet by walletId={}", walletId);

        ProgramWallet wallet = programWalletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.warn("Program wallet not found | walletId={}", walletId);
                    return new ResponseStatusException(
                            NOT_FOUND, "Program wallet not found"
                    );
                });

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setProgramId(wallet.getProgram().getProgramId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setGems(wallet.getGems());

        log.info(
                "Program wallet balance fetched | walletId={} | gems={}",
                walletId,
                wallet.getGems()
        );

        return dto;
    }

    public List<ProgramWalletBalanceDTO> getProgramWalletsByProgramId(UUID programId) {

        log.debug("Fetching all program wallets | programId={}", programId);

        List<ProgramWallet> wallets =
                programWalletRepository.findByProgramProgramId(programId);

        if (wallets.isEmpty()) {
            log.warn("No program wallets found | programId={}", programId);
            throw new ResponseStatusException(
                    NOT_FOUND, "No wallets found for this program"
            );
        }

        log.info(
                "Found {} program wallets | programId={}",
                wallets.size(),
                programId
        );

        return wallets.stream().map(wallet -> {
            ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
            dto.setProgramWalletId(wallet.getProgramWalletId());
            dto.setProgramId(wallet.getProgram().getProgramId());
            dto.setUserId(wallet.getUser().getUserId());
            dto.setGems(wallet.getGems());
            return dto;
        }).toList();
    }

    public ProgramWalletBalanceDTO getMyProgramWallet(UUID programId) {

        log.debug("Fetching my program wallet | programId={}", programId);

        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                user = userRepository.findById(p.userId()).orElse(null);
            } else {
                user = null;
            }
        } else {
            user = null;
        }

        if (user == null) {
            log.warn("Authenticated user not found while fetching program wallet");
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        ProgramWallet wallet =
                programWalletRepository
                        .findByUserUserIdAndProgramProgramId(
                                user.getUserId(),
                                programId
                        )
                        .orElseThrow(() -> {
                            log.warn(
                                    "Program wallet not found | userId={} | programId={}",
                                    user.getUserId(),
                                    programId
                            );
                            return new ResponseStatusException(
                                    NOT_FOUND,
                                    "Program wallet not found"
                            );
                        });

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setProgramId(programId);
        dto.setGems(wallet.getGems());

        log.info(
                "My program wallet fetched | userId={} | programId={} | gems={}",
                user.getUserId(),
                programId,
                wallet.getGems()
        );

        return dto;
    }
}
