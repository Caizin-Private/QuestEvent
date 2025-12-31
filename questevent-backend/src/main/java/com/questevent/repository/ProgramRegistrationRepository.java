package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, Long> {

    boolean existsByProgram_ProgramIdAndUser_UserId(Long programId, Long userId);

    List<ProgramRegistration> findByProgramProgramId(Long programId);

    List<ProgramRegistration> findByUserUserId(Long userId);

    long countByProgramProgramId(Long programId);
}


