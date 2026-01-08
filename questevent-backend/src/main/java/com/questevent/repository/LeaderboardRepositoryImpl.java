package com.questevent.repository;

import com.questevent.dto.LeaderboardDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class LeaderboardRepositoryImpl implements LeaderboardRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<LeaderboardDTO> getGlobalLeaderboard() {
        return em.createQuery("""
            SELECT new com.questevent.dto.LeaderboardDTO(
                u.userId,
                u.name,

                /* completedActivitiesCount */
                COUNT(DISTINCT CASE
                    WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                    THEN s.activityRegistration.activity.activityId
                END),

                /* gems (FORCE Long) */
                (w.gems * 1L),

                /* score (FORCE Double / BigDecimal-safe) */
                COALESCE(
                    (
                        0.5 * (
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
                    + 0.2 * (w.gems * 1.0),
                    0.0
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
        """, LeaderboardDTO.class).getResultList();
    }

    @Override
    public List<LeaderboardDTO> getProgramLeaderboard(UUID programId) {
        return em.createQuery("""
            SELECT new com.questevent.dto.LeaderboardDTO(
                pw.user.userId,
                pw.user.name,

                /* completedActivitiesCount */
                COUNT(DISTINCT CASE
                    WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                    THEN s.activityRegistration.activity.activityId
                END),

                /* program gems (FORCE Long) */
                (pw.gems * 1L),

                /* program score */
                (
                    0.5 * COUNT(DISTINCT CASE
                        WHEN s.reviewStatus = com.questevent.enums.ReviewStatus.APPROVED
                        THEN s.activityRegistration.activity.activityId
                    END)
                    + 0.5 * (pw.gems * 1.0)
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
            ORDER BY 5 DESC
        """, LeaderboardDTO.class)
                .setParameter("programId", programId)
                .getResultList();
    }
}
