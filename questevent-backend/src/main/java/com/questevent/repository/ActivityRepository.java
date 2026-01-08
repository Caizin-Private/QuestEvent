package com.questevent.repository;

import com.questevent.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByProgram_ProgramId(Long programId);
    List<Activity> findByProgram_ProgramIdAndIsCompulsoryTrue(Long programId);
}

