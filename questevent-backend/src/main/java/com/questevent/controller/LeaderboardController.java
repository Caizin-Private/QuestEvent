package com.questevent.controller;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.service.LeaderboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/global")
    public List<LeaderboardDTO> globalLeaderboard() {
        return leaderboardService.getGlobalLeaderboard();
    }
}
