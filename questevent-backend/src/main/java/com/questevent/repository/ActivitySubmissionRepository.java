package com.questevent.repository;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, Long> {

    List<ActivitySubmission>
    findByActivityRegistrationActivityActivityId(Long activityId);

    List<ActivitySubmission>
    findByReviewStatus(ReviewStatus status);

    List<ActivitySubmission>
    findByActivityRegistrationActivityProgramJudgeUserUserId(Long userId);

    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
            ReviewStatus status,
            Long userId
    );

    boolean existsByActivityRegistration(ActivityRegistration registration);

}
