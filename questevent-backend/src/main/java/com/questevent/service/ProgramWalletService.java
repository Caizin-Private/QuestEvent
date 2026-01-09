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

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class ProgramWalletService {

    private static final String USER_NOT_FOUND = "User not found";
    private static final String PROGRAM_NOT_FOUND = "Program not found";
    private static final String WALLET_ALREADY_EXISTS = "Program wallet already exists";
    private static final String WALLET_NOT_FOUND = "Program wallet not found";

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

    public ProgramWallet createWallet(Long userId, Long programId) {

        log.debug(
                "Create program wallet requested | userId={} | programId={}",
                userId,
                programId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found while creating program wallet | userId={}", userId);
                    return new ResponseStatusException(NOT_FOUND, USER_NOT_FOUND);
                });

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error("Program not found while creating program wallet | programId={}", programId);
                    return new ResponseStatusException(NOT_FOUND, PROGRAM_NOT_FOUND);
                });

        if (programWalletRepository.findByUserAndProgram(user, program).isPresent()) {
            log.warn(
                    "Program wallet already exists | userId={} | programId={}",
                    userId,
                    programId
            );
            throw new ResponseStatusException(CONFLICT, WALLET_ALREADY_EXISTS);
        }

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setProgram(program);
        programWallet.setGems(0);

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

        ProgramWallet wallet = programWalletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.warn("Program wallet not found | walletId={}", walletId);
                    return new ResponseStatusException(NOT_FOUND, WALLET_NOT_FOUND);
                });

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setProgramId(wallet.getProgram().getProgramId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setGems(wallet.getGems());

        return dto;
    }

    public List<ProgramWalletBalanceDTO> getProgramWalletsByProgramId(Long programId) {

        List<ProgramWallet> wallets =
                programWalletRepository.findByProgramProgramId(programId);

        if (wallets.isEmpty()) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "No wallets found for this program"
            );
        }

        return wallets.stream().map(wallet -> {
            ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
            dto.setProgramWalletId(wallet.getProgramWalletId());
            dto.setProgramId(wallet.getProgram().getProgramId());
            dto.setUserId(wallet.getUser().getUserId());
            dto.setGems(wallet.getGems());
            return dto;
        }).toList();
    }

    public ProgramWalletBalanceDTO getMyProgramWallet(Long programId) {

        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal p)) {
            throw new ResponseStatusException(NOT_FOUND, USER_NOT_FOUND);
        }

        User user = userRepository.findById(p.userId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, USER_NOT_FOUND));

        ProgramWallet wallet =
                programWalletRepository
                        .findByUserUserIdAndProgramProgramId(
                                user.getUserId(),
                                programId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                NOT_FOUND,
                                WALLET_NOT_FOUND
                        ));

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setProgramId(programId);
        dto.setGems(wallet.getGems());

        return dto;
    }
}
