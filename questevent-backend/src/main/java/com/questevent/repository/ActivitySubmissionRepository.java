package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, Long> {

    /**
     * Fetch all submissions of a user (history)
     */
    List<ActivitySubmission> findByUserUserId(Long userId);

    /**
     * Fetch all submissions for an activity (judge view)
     */
    List<ActivitySubmission> findByActivityActivityId(Long activityId);

    /**
     * Fetch submissions of a user for a specific activity
     * (used to show attempt history)
     */
    List<ActivitySubmission> findByActivityActivityIdAndUserUserId(
            Long activityId,
            Long userId
    );
}