package com.questevent.dto;

import lombok.Data;

@Data
public class LeaderboardDto {
    private Long userId;
    private String userName;
    private int gems;
}
