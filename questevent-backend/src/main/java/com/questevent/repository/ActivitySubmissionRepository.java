package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import com.questevent.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, UUID> {

    // Existing (keep)
    boolean existsByActivityRegistrationActivityRegistrationId(
            UUID activityRegistrationId
    );

    // Existing (keep)
    List<ActivitySubmission>
    findByActivityRegistrationActivityActivityId(
            UUID activityId
    );

    // Existing (keep)
    List<ActivitySubmission>
    findByReviewStatus(
            ReviewStatus reviewStatus
    );

    /* =====================================================
       🔑 REQUIRED FOR JUDGE APIs (ADD THESE)
       ===================================================== */

    // ✅ Pending submissions for a judge (judge-scoped)
    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
            ReviewStatus reviewStatus,
            Long judgeUserId
    );

    // ✅ Pending submissions for an activity (all judges / owner)
    List<ActivitySubmission>
    findByReviewStatusAndActivityRegistrationActivityActivityId(
            ReviewStatus reviewStatus,
            UUID activityId
    );

    // ✅ Pending submissions for an activity AND judge
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
}
