package com.questevent.dto;

import java.util.UUID;

public record LeaderboardDTO(
        UUID userId,
        String userName,
        Long completedActivitiesCount,
        Long gems,
        Number score
) {}
