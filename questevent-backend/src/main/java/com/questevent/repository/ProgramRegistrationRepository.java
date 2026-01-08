package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, UUID> {

    boolean existsByProgram_ProgramIdAndUser_UserId(UUID programId, UUID userId);

    List<ProgramRegistration> findByProgramProgramId(UUID programId);

    List<ProgramRegistration> findByUserUserId(UUID userId);

    Optional<ProgramRegistration> findByProgramProgramIdAndUserUserId(UUID programId, UUID userId);

    long countByProgramProgramId(UUID programId);
}