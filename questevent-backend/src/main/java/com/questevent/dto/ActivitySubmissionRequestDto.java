package com.questevent.dto;

import lombok.Data;

@Data

public class ActivitySubmissionRequestDto {

    private Long activityId;

    private Long userId;

    private String submissionUrl;

}
