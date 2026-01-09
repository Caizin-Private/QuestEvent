package com.questevent.repository;

import com.questevent.dto.LeaderboardDTO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaderboardRepository {

    List<LeaderboardDTO> getGlobalLeaderboard();

    List<LeaderboardDTO> getProgramLeaderboard(UUID programId);
}
