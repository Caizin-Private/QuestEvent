package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, UUID> {


    boolean existsByActivityRegistration_ActivityRegistrationId(
            UUID activityRegistrationId
    );

    Optional<ActivitySubmission>
    findByActivityRegistration_ActivityRegistrationId(UUID activityRegistrationId);


    Optional<ActivitySubmission>
    findByActivityRegistrationActivityActivityIdAndActivityRegistrationUserUserId(
            UUID activityId,
            Long userId
    );

    long countByReviewStatusAndActivityRegistration_Activity_Program_Judge_User_UserId(
            ReviewStatus reviewStatus,
            Long judgeUserId
    );


    List<ActivitySubmission>
    findByActivityRegistrationActivityActivityId(
            UUID activityId
    );

    // Existing (keep)
    List<ActivitySubmission>
    findByReviewStatus(
            ReviewStatus reviewStatus
    );



    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
            ReviewStatus reviewStatus,
            Long judgeUserId
    );


    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityActivityId(
            ReviewStatus reviewStatus,
            UUID activityId
    );


    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityActivityIdAndActivityRegistrationActivityProgramJudgeUserUserId(
            ReviewStatus reviewStatus,
            UUID activityId,
            Long judgeUserId
    );

    List<ActivitySubmission>
    findByActivityRegistrationActivityProgramJudgeUserUserId(
            Long judgeUserId
    );


    @Query("""
    SELECT s
    FROM ActivitySubmission s
    WHERE s.activityRegistration.activity.activityId = :activityId
      AND s.activityRegistration.user.userId = :userId
""")
    Optional<ActivitySubmission>
    findUserSubmissionForActivity(
            UUID activityId,
            Long userId
    );

}
