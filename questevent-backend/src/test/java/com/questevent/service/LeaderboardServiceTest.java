package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.LeaderboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        // Arrange
        List<LeaderboardDTO> mockLeaderboard = List.of(
                new LeaderboardDTO(1L, "Alice", 6L, 200L, 98.5),
                new LeaderboardDTO(2L, "Bob", 4L, 150L, 92.0)
        );

        when(leaderboardRepository.getGlobalLeaderboard())
                .thenReturn(mockLeaderboard);

        // Act
        List<LeaderboardDTO> result =
                leaderboardService.getGlobalLeaderboard();

        // Assert
        assertEquals(2, result.size());

        LeaderboardDTO first = result.get(0);
        assertEquals(1L, first.userId());
        assertEquals("Alice", first.userName());
        assertEquals(6L, first.completedActivitiesCount());
        assertEquals(200L, first.gems());
        assertEquals(98.5, first.score().doubleValue());

        verify(leaderboardRepository).getGlobalLeaderboard();
    }

    @Test
    void getProgramLeaderboard_shouldReturnProgramLeaderboard() {

        // Arrange
        Long programId = 10L;

        List<LeaderboardDTO> mockLeaderboard = List.of(
                new LeaderboardDTO(3L, "User3", 5L, 150L, 95.5),
                new LeaderboardDTO(4L, "User4", 4L, 120L, 90.0)
        );

        when(leaderboardRepository.getProgramLeaderboard(programId))
                .thenReturn(mockLeaderboard);

        // Act
        List<LeaderboardDTO> result =
                leaderboardService.getProgramLeaderboard(programId);

        // Assert
        assertEquals(2, result.size());

        LeaderboardDTO first = result.get(0);
        assertEquals("User3", first.userName());
        assertEquals(150L, first.gems());
        assertEquals(5L, first.completedActivitiesCount());
        assertEquals(95.5, first.score().doubleValue());

        verify(leaderboardRepository)
                .getProgramLeaderboard(programId);
    }
}
