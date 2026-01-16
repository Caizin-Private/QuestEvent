package com.questevent.service;

import com.questevent.dto.SubmissionDetailsResponseDTO;
import com.questevent.dto.SubmissionStatusResponseDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserSubmissionSummaryDTO;
import com.questevent.entity.ActivitySubmission;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.utils.SecurityUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionQueryServiceImpl implements SubmissionQueryService {

    private final ActivitySubmissionRepository submissionRepository;
    private final SecurityUserResolver securityUserResolver; // ✅ added

    @Override
    public SubmissionDetailsResponseDTO getSubmissionDetails(
            UUID activityId,
            Authentication ignored
    ) {
        UserPrincipal principal =
                securityUserResolver.getCurrentUserPrincipal();

        ActivitySubmission submission =
                submissionRepository
                        .findByActivityRegistrationActivityActivityIdAndActivityRegistrationUserUserId(
                                activityId,
                                principal.userId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Submission not found for this activity"
                                )
                        );

        return new SubmissionDetailsResponseDTO(
                submission.getSubmissionId(),
                activityId,
                submission.getSubmissionUrl(),
                submission.getReviewStatus(),
                submission.getAwardedGems(),
                submission.getReviewedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubmissionSummaryDTO> getMySubmissions(
            Authentication ignored
    ) {
        UserPrincipal principal =
                securityUserResolver.getCurrentUserPrincipal();

        List<ActivitySubmission> submissions =
                submissionRepository
                        .findAllByActivityRegistration_User_UserId(
                                principal.userId()
                        );

        return submissions.stream()
                .map(submission -> new UserSubmissionSummaryDTO(
                        submission.getSubmissionId(),
                        submission.getActivityRegistration()
                                .getActivity()
                                .getActivityId(),
                        submission.getActivityRegistration()
                                .getActivity()
                                .getActivityName(),
                        submission.getReviewStatus(),
                        submission.getSubmittedAt(),
                        submission.getReviewedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionStatusResponseDTO getSubmissionStatus(UUID activityId) {

        UserPrincipal principal =
                securityUserResolver.getCurrentUserPrincipal();

        Long userId = principal.userId();

        log.debug(
                "Fetching submission status | activityId={} | userId={}",
                activityId,
                userId
        );

        ActivitySubmission submission =
                submissionRepository
                        .findUserSubmissionForActivity(activityId, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Submission not found")
                        );

        return SubmissionStatusResponseDTO.builder()
                .submissionId(submission.getSubmissionId())
                .reviewStatus(submission.getReviewStatus())
                .submittedAt(submission.getSubmittedAt())
                .reviewedAt(submission.getReviewedAt())
                .awardedGems(submission.getAwardedGems())
                .build();
    }
}
