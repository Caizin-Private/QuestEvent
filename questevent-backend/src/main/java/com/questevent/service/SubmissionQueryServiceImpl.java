package com.questevent.service;

import com.questevent.dto.SubmissionStatusResponseDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.ActivitySubmission;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.repository.ActivitySubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionQueryServiceImpl implements SubmissionQueryService {

    private final ActivitySubmissionRepository submissionRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public SubmissionStatusResponseDTO getSubmissionStatus(UUID activityId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {

            log.warn("Authenticated user not found while fetching submission status");
            throw new ResourceNotFoundException("User not found");
        }

        Long userId = principal.userId();

        log.debug(
                "Fetching submission status | activityId={} | userId={}",
                activityId,
                userId
        );

        ActivitySubmission submission =
                submissionRepository
                        .findUserSubmissionForActivity(activityId, userId)
                        .orElseThrow(() -> {
                            log.warn(
                                    "Submission not found | activityId={} | userId={}",
                                    activityId,
                                    userId
                            );
                            return new ResourceNotFoundException("Submission not found");
                        });

        return SubmissionStatusResponseDTO.builder()
                .submissionId(submission.getSubmissionId())
                .reviewStatus(submission.getReviewStatus())
                .submittedAt(submission.getSubmittedAt())
                .reviewedAt(submission.getReviewedAt())
                .awardedGems(submission.getAwardedGems())
                .build();
    }
}
