package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data

public class ActivitySubmissionRequestDTO {

    private UUID activityId;

    private UUID userId;

    private String submissionUrl;

}
