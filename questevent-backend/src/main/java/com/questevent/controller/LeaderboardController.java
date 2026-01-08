package com.questevent.controller;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(
        name = "Leaderboard",
        description = "Leaderboard APIs for ranking users globally and per program"
)
public class LeaderboardController {

    private static final Logger log =
            LoggerFactory.getLogger(LeaderboardController.class);

    private final LeaderboardService leaderboardService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/global")
    public List<LeaderboardDTO> globalLeaderboard() {
        log.info("Fetching global leaderboard");

        List<LeaderboardDTO> leaderboard =
                leaderboardService.getGlobalLeaderboard();

        log.debug("Global leaderboard fetched, totalEntries={}",
                leaderboard.size());

        return leaderboard;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/program/{programId}")
    public List<LeaderboardDTO> programLeaderboard(
            @PathVariable Long programId
    ) {
        log.info("Fetching program leaderboard for programId={}", programId);

        List<LeaderboardDTO> leaderboard =
                leaderboardService.getProgramLeaderboard(programId);

        log.debug("Program leaderboard fetched for programId={}, totalEntries={}",
                programId, leaderboard.size());

        return leaderboard;
    }
}
