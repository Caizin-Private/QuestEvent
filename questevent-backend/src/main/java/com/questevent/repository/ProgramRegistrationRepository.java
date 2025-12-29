package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.ProgramWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramRegistrationRepository
        extends JpaRepository<ProgramRegistration, Long> {

    boolean existsByProgram_ProgramIdAndUser_UserId(
            Long programId,
            Long userId
    );
}
