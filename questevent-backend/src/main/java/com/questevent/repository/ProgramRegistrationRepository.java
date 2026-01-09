package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, Long> {

    boolean existsByProgram_ProgramIdAndUser_UserId(Long programId, Long userId);

    List<ProgramRegistration> findByProgramProgramId(Long programId);

    List<ProgramRegistration> findByUserUserId(Long userId);

    Optional<ProgramRegistration> findByProgramProgramIdAndUserUserId(Long programId, Long userId);

    long countByProgramProgramId(Long programId);
}