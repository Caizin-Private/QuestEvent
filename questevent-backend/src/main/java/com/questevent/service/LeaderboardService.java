package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public List<LeaderboardDTO> getGlobalLeaderboard() {

        log.debug("Fetching global leaderboard");

        List<LeaderboardDTO> leaderboard =
                leaderboardRepository.getGlobalLeaderboard();

        if (leaderboard == null || leaderboard.isEmpty()) {
            log.warn("Global leaderboard is empty");
            throw new IllegalStateException("Global leaderboard is empty");
        }

        log.info(
                "Global leaderboard fetched successfully | entries={}",
                leaderboard.size()
        );

        return leaderboard;
    }

    public List<LeaderboardDTO> getProgramLeaderboard(Long programId) {

        log.debug(
                "Fetching program leaderboard | programId={}",
                programId
        );

        if (programId == null || programId <= 0) {
            log.error("Invalid programId supplied for leaderboard | programId={}", programId);
            throw new IllegalArgumentException("Program ID must be greater than zero");
        }

        List<LeaderboardDTO> leaderboard =
                leaderboardRepository.getProgramLeaderboard(programId);

        if (leaderboard == null || leaderboard.isEmpty()) {
            log.warn(
                    "Program leaderboard empty | programId={}",
                    programId
            );
            throw new IllegalStateException(
                    "No leaderboard data found for programId: " + programId
            );
        }

        log.info(
                "Program leaderboard fetched successfully | programId={} | entries={}",
                programId,
                leaderboard.size()
        );

        return leaderboard;
    }
}
