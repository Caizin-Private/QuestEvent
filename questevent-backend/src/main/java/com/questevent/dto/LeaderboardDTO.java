package com.questevent.dto;

public record LeaderboardDTO(
        Long userId,
        String userName,
        Long participationCount,
        Long gems,
        Double score
) {}
