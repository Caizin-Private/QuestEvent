package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.LeaderboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;


    @Test
    void getGlobalLeaderboard_shouldReturnGlobalLeaderboard() {

        List<LeaderboardDTO> mockLeaderboard = List.of(
                new LeaderboardDTO(1L, "Alice", 6L, 200L, 98.5),
                new LeaderboardDTO(2L, "Bob", 4L, 150L, 92.0)
        );

        when(leaderboardRepository.getGlobalLeaderboard())
                .thenReturn(mockLeaderboard);

        List<LeaderboardDTO> result =
                leaderboardService.getGlobalLeaderboard();

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).userName());

        verify(leaderboardRepository).getGlobalLeaderboard();
    }

    @Test
    void getGlobalLeaderboard_shouldThrowWhenEmpty() {

        when(leaderboardRepository.getGlobalLeaderboard())
                .thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> leaderboardService.getGlobalLeaderboard()
        );

        assertEquals("Global leaderboard is empty", ex.getMessage());
    }



    @Test
    void getProgramLeaderboard_shouldReturnProgramLeaderboard() {

        Long programId = 10L;

        List<LeaderboardDTO> mockLeaderboard = List.of(
                new LeaderboardDTO(3L, "User3", 5L, 150L, 95.5),
                new LeaderboardDTO(4L, "User4", 4L, 120L, 90.0)
        );

        when(leaderboardRepository.getProgramLeaderboard(programId))
                .thenReturn(mockLeaderboard);

        List<LeaderboardDTO> result =
                leaderboardService.getProgramLeaderboard(programId);

        assertEquals(2, result.size());
        assertEquals("User3", result.get(0).userName());

        verify(leaderboardRepository)
                .getProgramLeaderboard(programId);
    }

    @Test
    void getProgramLeaderboard_shouldThrowWhenEmpty() {

        Long programId = 10L;

        when(leaderboardRepository.getProgramLeaderboard(programId))
                .thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> leaderboardService.getProgramLeaderboard(programId)
        );

        assertEquals(
                "No leaderboard data found for programId: " + programId,
                ex.getMessage()
        );
    }

    @Test
    void getProgramLeaderboard_shouldThrowWhenProgramIdInvalid() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> leaderboardService.getProgramLeaderboard(0L)
        );

        assertEquals("Program ID must be greater than zero", ex.getMessage());
    }
}
