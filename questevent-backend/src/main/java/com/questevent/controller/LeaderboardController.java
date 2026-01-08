package com.questevent.controller;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(
        name = "Leaderboardd",
        description = "Leaderboard APIs for ranking users globally and per program"
)
public class LeaderboardController {

    private final LeaderboardService leaderboardService;


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/global")
    @Operation(
            summary = "Get global leaderboard",
            description = "Returns a ranked leaderboard of all users based on gems and program participation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Global leaderboard fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<LeaderboardDTO> globalLeaderboard() {
        return leaderboardService.getGlobalLeaderboard();
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/program/{programId}")
    @Operation(
            summary = "Get program leaderboard",
            description = "Returns a ranked leaderboard for a specific program"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Program leaderboard fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Program not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<LeaderboardDTO> programLeaderboard(
            @Parameter(
                    description = "Program ID for which leaderboard is required",
                    required = true,
                    example = "1"
            )
            @PathVariable Long programId
    ) {
        return leaderboardService.getProgramLeaderboard(programId);
    }
}