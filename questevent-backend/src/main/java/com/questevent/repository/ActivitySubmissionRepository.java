package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, Long> {


    boolean existsByActivityRegistration_ActivityRegistrationId(Long activityRegistrationId);

    List<ActivitySubmission>
    findByActivityRegistrationActivityActivityId(Long activityId);

    List<ActivitySubmission> findByReviewStatus(ReviewStatus reviewStatus);

}