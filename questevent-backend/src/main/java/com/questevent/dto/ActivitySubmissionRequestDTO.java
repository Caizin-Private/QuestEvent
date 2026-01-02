package com.questevent.dto;

import lombok.Data;

@Data

public class ActivitySubmissionRequestDTO {

    private Long activityId;

    private Long userId;

    private String submissionUrl;

}
