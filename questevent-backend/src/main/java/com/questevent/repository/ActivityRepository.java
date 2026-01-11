package com.questevent.repository;

import com.questevent.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByProgramProgramId(UUID programId);
    List<Activity> findByProgramProgramIdAndIsCompulsoryTrue(UUID programId);
}

