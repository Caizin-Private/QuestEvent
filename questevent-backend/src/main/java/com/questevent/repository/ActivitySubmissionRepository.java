package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, UUID> {


    boolean existsByActivityRegistration_ActivityRegistrationId(UUID activityRegistrationId);

    List<ActivitySubmission>
    findByActivityRegistrationActivityActivityId(UUID activityId);

    List<ActivitySubmission> findByReviewStatus(ReviewStatus reviewStatus);

}