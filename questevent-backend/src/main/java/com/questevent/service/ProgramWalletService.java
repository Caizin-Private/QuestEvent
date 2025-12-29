package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}