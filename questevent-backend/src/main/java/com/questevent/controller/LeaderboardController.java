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
    @Operation(
            summary = "Get global leaderboard",
            description = "Returns a ranked leaderboard of all users based on gems and program participation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Global leaderboard fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(
            summary = "Get program leaderboard",
            description = "Returns a ranked leaderboard for a specific program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program leaderboard fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Program not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<LeaderboardDTO> programLeaderboard(
            @Parameter(
                    description = "Program ID for which leaderboard is required",
                    required = true
            )
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
