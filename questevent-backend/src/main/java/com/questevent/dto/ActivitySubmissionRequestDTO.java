package com.questevent.dto;

import lombok.Data;

import java.util.UUID;

@Data

public class ActivitySubmissionRequestDTO {

    private UUID activityId;

    private String submissionUrl;

}
