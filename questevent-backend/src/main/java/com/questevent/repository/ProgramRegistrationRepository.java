package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, UUID> {

    boolean existsByProgramProgramIdAndUserUserId(UUID programId, Long userId);

    List<ProgramRegistration> findByProgramProgramId(UUID programId);

    List<ProgramRegistration> findByUserUserId(Long userId);

    Optional<ProgramRegistration> findByProgramProgramIdAndUserUserId(UUID programId, Long userId);

    long countByProgramProgramId(UUID programId);
}