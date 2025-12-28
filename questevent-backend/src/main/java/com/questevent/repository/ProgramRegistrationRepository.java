package com.questevent.repository;

import com.questevent.entity.ProgramRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRegistrationRepository extends JpaRepository<ProgramRegistration, Long> {
    boolean existsByProgramIdAndUserId(Long programId, Long userId);
}
