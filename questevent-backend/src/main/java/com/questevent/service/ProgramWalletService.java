package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    public ProgramWallet createWallet(Long userId, Long programId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (programWalletRepository.findByUserAndProgram(user, program).isPresent()) {
            throw new RuntimeException("ProgramWallet already exists");
        }

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setProgram(program);
        programWallet.setGems(0);

        return programWalletRepository.save(programWallet);
    }

    public List<ProgramWalletBalanceDTO> getUserProgramWalletBalances(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "User not found"
                ));

        List<ProgramWallet> wallets =
                programWalletRepository.findByUser(user);

        if (wallets.isEmpty()) {
            throw new ResponseStatusException(
                    NOT_FOUND, "No program wallets found for user"
            );
        }

        return wallets.stream().map(wallet -> {
            ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
            dto.setProgramWalletId(wallet.getProgramWalletId());
            dto.setGems(wallet.getGems());
            return dto;
        }).toList();
    }


    public ProgramWalletBalanceDTO getWalletBalanceByWalletId(UUID walletId) {

        ProgramWallet wallet = programWalletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Program wallet not found"
                ));

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
                    NOT_FOUND, "No wallets found for this program"
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

        User user = null;

        // Reuse RBAC logic style
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                user = userRepository.findById(p.userId()).orElse(null);
            }
        }

        if (user == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        ProgramWallet wallet =
                programWalletRepository
                        .findByUserUserIdAndProgramProgramId(
                                user.getUserId(),
                                programId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                NOT_FOUND,
                                "Program wallet not found"
                        ));

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setUserId(wallet.getUser().getUserId());
        dto.setProgramId(programId);
        dto.setGems(wallet.getGems());

        return dto;
    }


}