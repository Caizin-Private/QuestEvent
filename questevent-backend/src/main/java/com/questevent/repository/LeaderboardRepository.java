package com.questevent.repository;

import com.questevent.dto.LeaderboardDto;
import com.questevent.entity.ProgramWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<ProgramWallet, Long> {

    //GLOBAL LEADERBOARD
    @Query("""
        SELECT new com.questevent.dto.LeaderboardDto(
            u.userId,
            u.name,
            COUNT(DISTINCT pr.program.programId),
            uw.gems,
            (0.7 * uw.gems + 0.3 * COUNT(DISTINCT pr.program.programId))
        )
        FROM User u
        JOIN u.wallet uw
        LEFT JOIN u.programRegistrations pr
        GROUP BY u.userId, u.name, uw.gems
        ORDER BY (0.7 * uw.gems + 0.3 * COUNT(DISTINCT pr.program.programId)) DESC
    """)
    List<LeaderboardDto> getGlobalLeaderboard();


    //PROGRAM LEADERBOARD
    @Query("""
        SELECT new com.questevent.dto.LeaderboardDto(
            pw.user.userId,
            pw.user.name,
            COUNT(ar.activityRegistrationId),
            pw.gems,
            (0.7 * pw.gems + 0.3 * COUNT(ar.activityRegistrationId))
        )
        FROM ProgramWallet pw
        LEFT JOIN ActivityRegistration ar
            ON ar.user.userId = pw.user.userId
            AND ar.activity.program.programId = :programId
        WHERE pw.program.programId = :programId
        GROUP BY pw.user.userId, pw.user.name, pw.gems
        ORDER BY (0.7 * pw.gems + 0.3 * COUNT(ar.activityRegistrationId)) DESC
    """)
    List<LeaderboardDto> getProgramLeaderboard(Long programId);
}
