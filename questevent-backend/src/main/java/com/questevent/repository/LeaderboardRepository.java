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
    
        /* Total completed activities */
        COUNT(DISTINCT CASE
            WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
            THEN ar.activity.activityId
        END),
    
        /* Total gems */
        uw.gems,
    
        /* GLOBAL SCORE */
        (
            0.5 * (
                (COUNT(DISTINCT CASE
                    WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
                    THEN ar.activity.activityId
                END) * 1.0)
                /
                COUNT(DISTINCT ar.activity.activityId)
                * 100
            )
            + 0.3 * COUNT(DISTINCT CASE
                WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
                THEN ar.activity.activityId
            END)
            + 0.2 * uw.gems
        )
    )
    FROM User u
    JOIN u.wallet uw
    LEFT JOIN ActivityRegistration ar
        ON ar.user.userId = u.userId
    GROUP BY u.userId, u.name, uw.gems
    ORDER BY
    (
        0.5 * (
            (COUNT(DISTINCT CASE
                WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
                THEN ar.activity.activityId
            END) * 1.0)
            /
            COUNT(DISTINCT ar.activity.activityId)
            * 100
        )
        + 0.3 * COUNT(DISTINCT CASE
            WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
            THEN ar.activity.activityId
        END)
        + 0.2 * uw.gems
    ) DESC
    """)
        List<LeaderboardDTO> getGlobalLeaderboard();


    //program leaderboard
    @Query("""
    SELECT new com.questevent.dto.LeaderboardDTO(
        pw.user.userId,
        pw.user.name,

        /* Completed activities in this program */
        COUNT(DISTINCT CASE
            WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
            THEN ar.activity.activityId
        END),

        /* Program-specific gems */
        pw.gems,

        /* PROGRAM SCORE (50–50) */
        (
            0.5 * COUNT(DISTINCT CASE
                WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
                THEN ar.activity.activityId
            END)
            + 0.5 * pw.gems
        )
    )
    FROM ProgramWallet pw
    LEFT JOIN ActivityRegistration ar
        ON ar.user.userId = pw.user.userId
        AND ar.activity.program.programId = :programId
    WHERE pw.program.programId = :programId
    GROUP BY pw.user.userId, pw.user.name, pw.gems
    ORDER BY
    (
        0.5 * COUNT(DISTINCT CASE
            WHEN ar.completionStatus = com.questevent.enums.CompletionStatus.COMPLETED
            THEN ar.activity.activityId
        END)
        + 0.5 * pw.gems
    ) DESC
    """)
        List<LeaderboardDTO> getProgramLeaderboard(Long programId);
}



