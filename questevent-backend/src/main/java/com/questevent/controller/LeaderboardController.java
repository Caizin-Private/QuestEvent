package com.questevent.controller;

import com.questevent.dto.LeaderboardDto;
import com.questevent.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    //Global leaderboard
    @GetMapping("/global")
    public List<LeaderboardDto> globalLeaderboard() {
        return leaderboardService.getGlobalLeaderboard();
    }

    //Program leaderboard
    @GetMapping("/program/{programId}")
    public List<LeaderboardDto> programLeaderboard(
            @PathVariable Long programId
    ) {
        return leaderboardService.getProgramLeaderboard(programId);
    }
}
