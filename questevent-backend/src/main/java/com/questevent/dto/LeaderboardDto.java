package com.questevent.dto;

public record LeaderboardDto(
        Long userId,
        String userName,
        Long participationCount,
        Long gems,
        Double score
) {}
