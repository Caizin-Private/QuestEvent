package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.LeaderboardRepository;
import com.questevent.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    //global service
    private final LeaderboardRepository leaderboardRepository;

    public List<LeaderboardDTO> getGlobalLeaderboard() {

        List<LeaderboardDTO> leaderboard =
                leaderboardRepository.getGlobalLeaderboard();

        if (leaderboard == null || leaderboard.isEmpty()) {
            throw new IllegalStateException("Global leaderboard is empty");
        }

        return leaderboard;
    }

    //program service
    public List<LeaderboardDTO> getProgramLeaderboard(Long programId) {

        if (programId == null || programId <= 0) {
            throw new IllegalArgumentException("Program ID must be greater than zero");
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