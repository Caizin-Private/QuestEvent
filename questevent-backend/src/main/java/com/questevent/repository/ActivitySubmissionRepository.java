package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, Long> {

    Optional<ActivitySubmission> findByActivityActivityIdAndUserUserId(
            Long activityId,
            Long userId
    );

    List<ActivitySubmission> findByActivityActivityId(Long activityId);

    List<ActivitySubmission> findByUserUserId(Long userId);
}