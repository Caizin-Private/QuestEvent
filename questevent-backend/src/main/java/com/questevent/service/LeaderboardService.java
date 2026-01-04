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

    private final LeaderboardRepository leaderboardRepository;

    public List<LeaderboardDTO> getGlobalLeaderboard() {
        return leaderboardRepository.getGlobalLeaderboard();
    }

    public List<LeaderboardDTO> getProgramLeaderboard(Long programId) {
        return leaderboardRepository.getProgramLeaderboard(programId);
    }
}