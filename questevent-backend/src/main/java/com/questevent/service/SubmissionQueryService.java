package com.questevent.service;

import com.questevent.dto.SubmissionStatusResponseDTO;

import java.util.UUID;

public interface SubmissionQueryService {
    SubmissionStatusResponseDTO getSubmissionStatus(UUID activityId);
}
