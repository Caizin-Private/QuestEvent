package com.questevent.repository;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<ProgramWallet, Long> {

    //global leaderboard
    @Query("""
    SELECT new com.questevent.dto.LeaderboardDTO(
        u.userId,
        u.name,
    
        /* completedActivitiesCount (judge-approved only) */
        COUNT(DISTINCT CASE
            WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
            THEN s.activityRegistration.activity.activityId
        END),
    
        /* total gems */
        w.gems,
    
        /* final score */
        COALESCE(
            (
                0.5 * (
                    (
                        COUNT(DISTINCT CASE
                            WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                            THEN s.activityRegistration.activity.activityId
                        END) * 1.0
                    )
                    /
                    NULLIF(COUNT(DISTINCT ar.activity.activityId), 0)
                    * 100
                )
                + 0.3 * COUNT(DISTINCT CASE
                    WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                    THEN s.activityRegistration.activity.activityId
                END)
                + 0.2 * w.gems
            ),
            0
        )
    )
    FROM User u
    JOIN u.wallet w
    LEFT JOIN ActivityRegistration ar
        ON ar.user.userId = u.userId
    LEFT JOIN ActivitySubmission s
        ON s.activityRegistration.activityRegistrationId = ar.activityRegistrationId
    GROUP BY u.userId, u.name, w.gems
    ORDER BY 5 DESC
    """)
        List<LeaderboardDTO> getGlobalLeaderboard();






    //program leaderboard
    @Query("""
    SELECT new com.questevent.dto.LeaderboardDTO(
        pw.user.userId,
        pw.user.name,
    
        /* Completed activities (judge approved) */
        COUNT(DISTINCT CASE
            WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
            THEN s.activityRegistration.activity.activityId
        END),
    
        /* Program-specific gems */
        pw.gems,
    
        /* PROGRAM SCORE (50–50) */
        (
            0.5 * COUNT(DISTINCT CASE
                WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                THEN s.activityRegistration.activity.activityId
            END)
            + 0.5 * pw.gems
        )
    )
    FROM ProgramWallet pw
    LEFT JOIN ActivityRegistration ar
        ON ar.user.userId = pw.user.userId
        AND ar.activity.program.programId = :programId
    LEFT JOIN ActivitySubmission s
        ON s.activityRegistration.activityRegistrationId = ar.activityRegistrationId
    WHERE pw.program.programId = :programId
    GROUP BY pw.user.userId, pw.user.name, pw.gems
    ORDER BY
    (
        0.5 * COUNT(DISTINCT CASE
            WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
            THEN s.activityRegistration.activity.activityId
        END)
        + 0.5 * pw.gems
    ) DESC
    """)
        List<LeaderboardDTO> getProgramLeaderboard(Long programId);

}



