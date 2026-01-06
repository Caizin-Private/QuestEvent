package com.questevent.controller;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardController leaderboardController;

    // =========================================================
    // 🌍 GLOBAL LEADERBOARD UNIT TEST CASES
    // =========================================================

    @Test
    void shouldReturnGlobalLeaderboard() {

        List<LeaderboardDTO> mockResponse = List.of(
                new LeaderboardDTO(1L, "Alice", 10L, 50, 30.0),
                new LeaderboardDTO(2L, "Bob", 8L, 40, 24.0)
        );

        when(leaderboardService.getGlobalLeaderboard())
                .thenReturn(mockResponse);

        List<LeaderboardDTO> result =
                leaderboardController.globalLeaderboard();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).userName());
        assertEquals(10L, result.get(0).completedActivitiesCount());

        verify(leaderboardService).getGlobalLeaderboard();
    }

    @Test
    void shouldReturnEmptyGlobalLeaderboard() {

        when(leaderboardService.getGlobalLeaderboard())
                .thenReturn(List.of());

        List<LeaderboardDTO> result =
                leaderboardController.globalLeaderboard();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(leaderboardService).getGlobalLeaderboard();
    }

    @Test
    void shouldCallServiceForGlobalLeaderboard() {

        leaderboardController.globalLeaderboard();

        verify(leaderboardService).getGlobalLeaderboard();
    }

    // =========================================================
    // 🏷 PROGRAM LEADERBOARD UNIT TEST CASES
    // =========================================================

    @Test
    void shouldReturnProgramLeaderboard() {

        Long programId = 1L;

        List<LeaderboardDTO> mockResponse = List.of(
                new LeaderboardDTO(1L, "Alice", 6L, 30, 18.0),
                new LeaderboardDTO(2L, "Bob", 4L, 20, 12.0)
        );

        when(leaderboardService.getProgramLeaderboard(programId))
                .thenReturn(mockResponse);

        List<LeaderboardDTO> result =
                leaderboardController.programLeaderboard(programId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Bob", result.get(1).userName());
        assertEquals(4L, result.get(1).completedActivitiesCount());

        verify(leaderboardService).getProgramLeaderboard(programId);
    }

    @Test
    void shouldReturnEmptyProgramLeaderboard() {

        Long programId = 99L;

        when(leaderboardService.getProgramLeaderboard(programId))
                .thenReturn(List.of());

        List<LeaderboardDTO> result =
                leaderboardController.programLeaderboard(programId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(leaderboardService).getProgramLeaderboard(programId);
    }

    @Test
    void shouldCallServiceWithCorrectProgramId() {

        Long programId = 5L;

        leaderboardController.programLeaderboard(programId);

        verify(leaderboardService).getProgramLeaderboard(programId);
    }
}
