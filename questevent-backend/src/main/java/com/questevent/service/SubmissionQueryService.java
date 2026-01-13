package com.questevent.service;

import com.questevent.dto.SubmissionDetailsResponseDTO;
import com.questevent.dto.SubmissionStatusResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface SubmissionQueryService {
    SubmissionStatusResponseDTO getSubmissionStatus(UUID activityId);

    SubmissionDetailsResponseDTO getSubmissionDetails(
            UUID activityId,
            Authentication authentication
    );
}
