package com.questevent.dto;

import java.util.UUID;

public record LeaderboardDTO(
        Long userId,
        String userName,
        Long completedActivitiesCount,
        Long gems,
        Number score
) {}
