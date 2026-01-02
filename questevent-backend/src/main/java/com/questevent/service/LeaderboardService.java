package com.questevent.service;

import com.questevent.dto.LeaderboardDto;
import com.questevent.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public List<LeaderboardDto> getGlobalLeaderboard() {
        return leaderboardRepository.getGlobalLeaderboard();
    }

    public List<LeaderboardDto> getProgramLeaderboard(Long programId) {
        return leaderboardRepository.getProgramLeaderboard(programId);
    }
}
