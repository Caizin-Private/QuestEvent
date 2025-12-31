package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDto;
import com.questevent.dto.WalletBalanceDto;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
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

    public List<ProgramWalletBalanceDto> getWalletBalance(Long userId) {

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
            ProgramWalletBalanceDto dto = new ProgramWalletBalanceDto();
            dto.setProgramWalletId(wallet.getProgramWalletId());
            dto.setGems(wallet.getGems());
            return dto;
        }).toList();
    }


    public ProgramWalletBalanceDto getWalletBalanceByWalletId(UUID walletId) {

        ProgramWallet wallet = programWalletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Program wallet not found"
                ));

        ProgramWalletBalanceDto dto = new ProgramWalletBalanceDto();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setGems(wallet.getGems());

        return dto;
    }

    @Transactional
    public void programWalletSettlement(Long programId) {

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

        }

        program.setStatus(ProgramStatus.SETTLED);
    }

}