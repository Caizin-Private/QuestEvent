package com.questevent.service;

import com.questevent.dto.SubmissionDetailsResponseDTO;
import com.questevent.dto.SubmissionStatusResponseDTO;
import com.questevent.dto.UserSubmissionSummaryDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface SubmissionQueryService {
    SubmissionStatusResponseDTO getSubmissionStatus(UUID activityId);

    SubmissionDetailsResponseDTO getSubmissionDetails(
            UUID activityId,
            Authentication authentication
    );

    List<UserSubmissionSummaryDTO> getMySubmissions(Authentication authentication);

}
