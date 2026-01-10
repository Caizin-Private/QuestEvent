package com.questevent.dto;


public record LeaderboardDTO(
        Long userId,
        String userName,
        Long completedActivitiesCount,
        Long gems,
        Number score
) {}
