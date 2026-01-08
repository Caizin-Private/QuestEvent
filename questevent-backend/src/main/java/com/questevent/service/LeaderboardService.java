package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.LeaderboardRepository;
import com.questevent.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public List<LeaderboardDTO> getGlobalLeaderboard() {

        List<LeaderboardDTO> leaderboard =
                leaderboardRepository.getGlobalLeaderboard();

        if (leaderboard == null || leaderboard.isEmpty()) {
            throw new IllegalStateException("Global leaderboard is empty");
        }

        return leaderboard;
    }

    public List<LeaderboardDTO> getProgramLeaderboard(UUID programId) {

        if (programId == null ) {
            throw new IllegalArgumentException("Program ID cannot be null or negative");
        }

        List<LeaderboardDTO> leaderboard =
                leaderboardRepository.getProgramLeaderboard(programId);

        if (leaderboard == null || leaderboard.isEmpty()) {
            throw new IllegalStateException(
                    "No leaderboard data found for programId: " + programId
            );
        }

        return leaderboard;
    }
}