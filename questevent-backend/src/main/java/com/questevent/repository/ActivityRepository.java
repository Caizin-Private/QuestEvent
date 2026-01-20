package com.questevent.repository;

import com.questevent.dto.ActivityWithRegistrationStatusDTO;
import com.questevent.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByProgram_ProgramId(UUID programId);
    List<Activity> findByProgram_ProgramIdAndIsCompulsoryTrue(UUID programId);

    @Query("""
        SELECT new com.questevent.dto.ActivityWithRegistrationStatusDTO(
            a.activityId,
            a.activityName,
            CASE WHEN ar.activityRegistrationId IS NOT NULL THEN true ELSE false END,
            ar.completionStatus
        )
        FROM Activity a
        LEFT JOIN ActivityRegistration ar
            ON ar.activity = a AND ar.user.userId = :userId
        WHERE a.program.programId = :programId
    """)
    List<ActivityWithRegistrationStatusDTO> findActivitiesForUser(
            UUID programId,
            Long userId
    );

}

