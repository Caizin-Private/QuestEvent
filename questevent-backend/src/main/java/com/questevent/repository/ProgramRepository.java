package com.questevent.repository;

import com.questevent.entity.Program;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findByUserUserId(Long userId);

    List<Program> findByStatusAndEndDateBefore(ProgramStatus status, LocalDateTime now);

    List<Program> findByStatusAndDepartment(ProgramStatus status, Department department);

    List<Program> findByStatusAndUserUserId(ProgramStatus status, Long userId);

    @Query("SELECT p FROM Program p WHERE p.judge.user.userId = :userId")
    List<Program> findByJudgeUserId(@Param("userId") Long userId);
}
