package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.exception.WalletNotFoundException;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ProgramWalletService {

    private final ProgramWalletRepository programWalletRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final SecurityUserResolver securityUserResolver; // ✅ added

    public ProgramWalletService(
            ProgramWalletRepository programWalletRepository,
            UserRepository userRepository,
            ProgramRepository programRepository,
            SecurityUserResolver securityUserResolver
    ) {
        this.programWalletRepository = programWalletRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.securityUserResolver = securityUserResolver;
    }

    public ProgramWallet createWallet(Long userId, UUID programId) {

        log.debug(
                "Create program wallet requested | userId={} | programId={}",
                userId,
                programId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new ProgramNotFoundException("Program not found")
                );

        if (programWalletRepository.findByUserAndProgram(user, program).isPresent()) {
            throw new ResourceConflictException("Program wallet already exists");
        }

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setProgram(program);
        programWallet.setGems(0L);

        return programWalletRepository.save(programWallet);
    }

    public ProgramWalletBalanceDTO getWalletBalanceByWalletId(UUID walletId) {

        ProgramWallet wallet = programWalletRepository.findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Program wallet not found")
                );

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setProgramId(wallet.getProgram().getProgramId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setGems(wallet.getGems());

        return dto;
    }

    public List<ProgramWalletBalanceDTO> getProgramWalletsByProgramId(UUID programId) {

        List<ProgramWallet> wallets =
                programWalletRepository.findByProgramProgramId(programId);

        if (wallets.isEmpty()) {
            throw new WalletNotFoundException("No wallets found for this program");
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

    public ProgramWalletBalanceDTO getMyProgramWallet(UUID programId) {

        log.debug("Fetching my program wallet | programId={}", programId);

        User user = securityUserResolver.getCurrentUser();

        ProgramWallet wallet =
                programWalletRepository
                        .findByUserUserIdAndProgramProgramId(
                                user.getUserId(),
                                programId
                        )
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Program wallet not found"
                                )
                        );

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setProgramId(programId);
        dto.setGems(wallet.getGems());

        return dto;
    }
}
