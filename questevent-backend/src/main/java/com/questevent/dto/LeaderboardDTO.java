package com.questevent.dto;

import lombok.Data;

@Data
public class LeaderboardDTO {
    private Long userId;
    private String userName;
    private int gems;
}
