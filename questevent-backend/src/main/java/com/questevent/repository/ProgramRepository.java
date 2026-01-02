package com.questevent.repository;

import com.questevent.entity.Program;
import com.questevent.enums.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByUser_UserId(Long userId);

    List<Program> findByStatusAndEndDateBefore(
            ProgramStatus status,
            LocalDateTime now
    );
}
