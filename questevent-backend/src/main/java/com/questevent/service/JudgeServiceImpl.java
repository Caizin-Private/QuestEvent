package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final ProgramWalletTransactionService programWalletTransactionService;
    private final UserRepository userRepository;

    private User resolveCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt to judge service");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized"
            );
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal p) {
            return userRepository.findById(p.userId())
                    .orElseThrow(() -> {
                        log.error("Authenticated user not found | userId={}", p.userId());
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        );
                    });
        }

        log.warn("Invalid authentication principal");
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        List<JudgeSubmissionDTO> result =
                user.getRole() == Role.OWNER
                        ? submissionRepository
                        .findByReviewStatus(ReviewStatus.PENDING)
                        .stream()
                        .map(this::mapToDto)
                        .toList()
                        : submissionRepository
                        .findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
                                ReviewStatus.PENDING,
                                user.getUserId()
                        )
                        .stream()
                        .map(this::mapToDto)
                        .toList();

        log.info(
                "Pending submissions fetched | userId={} | count={}",
                user.getUserId(),
                result.size()
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForActivity(
            UUID activityId,
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        List<JudgeSubmissionDTO> result =
                user.getRole() == Role.OWNER
                        ? submissionRepository
                        .findByReviewStatusAndActivityRegistrationActivityActivityId(
                                ReviewStatus.PENDING,
                                activityId
                        )
                        .stream()
                        .map(this::mapToDto)
                        .toList()
                        : submissionRepository
                        .findByReviewStatusAndActivityRegistrationActivityActivityIdAndActivityRegistrationActivityProgramJudgeUserUserId(
                                ReviewStatus.PENDING,
                                activityId,
                                user.getUserId()
                        )
                        .stream()
                        .map(this::mapToDto)
                        .toList();

        log.info(
                "Pending submissions fetched | activityId={} | count={}",
                activityId,
                result.size()
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        List<JudgeSubmissionDTO> result =
                user.getRole() == Role.OWNER
                        ? submissionRepository.findAll()
                        .stream()
                        .map(this::mapToDto)
                        .toList()
                        : submissionRepository
                        .findByActivityRegistrationActivityProgramJudgeUserUserId(
                                user.getUserId()
                        )
                        .stream()
                        .map(this::mapToDto)
                        .toList();

        log.info(
                "All submissions fetched | userId={} | count={}",
                user.getUserId(),
                result.size()
        );

        return result;
    }

    @Override
    @Transactional
    public void reviewSubmission(UUID submissionId) {

        log.debug("Review submission requested | submissionId={}", submissionId);

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> {
                    log.warn("Submission not found | submissionId={}", submissionId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Submission not found"
                    );
                });

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Submission already reviewed"
            );
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        Activity activity = registration.getActivity();
        Program program = activity.getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Judge not assigned to this program"
            );
        }

        Long rewardGems = activity.getRewardGems();
        if (rewardGems == null || rewardGems < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reward gems"
            );
        }

        submission.setReviewedBy(judge);
        submission.setReviewStatus(ReviewStatus.APPROVED);
        submission.setAwardedGems(rewardGems);
        submission.setReviewedAt(Instant.now());
        submissionRepository.save(submission);

        registration.setCompletionStatus(CompletionStatus.COMPLETED);
        registrationRepository.save(registration);

        programWalletTransactionService.creditGems(
                registration.getUser(),
                program,
                rewardGems
        );
    }

    @Override
    @Transactional
    public void rejectSubmission(
            UUID submissionId,
            String rejectionReason
    ) {
        log.debug("Reject submission requested | submissionId={}", submissionId);

        ActivitySubmission submission =
                submissionRepository.findById(submissionId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Submission not found"
                        ));

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Submission already reviewed"
            );
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        Program program = registration.getActivity().getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Judge not assigned to this program"
            );
        }

        submission.setReviewStatus(ReviewStatus.REJECTED);
        submission.setReviewedBy(judge);
        submission.setReviewedAt(Instant.now());
        submission.setRejectionReason(rejectionReason);

        submissionRepository.save(submission);
    }

    private JudgeSubmissionDTO mapToDto(ActivitySubmission submission) {

        ActivityRegistration reg = submission.getActivityRegistration();

        return new JudgeSubmissionDTO(
                submission.getSubmissionId(),
                reg.getActivity().getActivityId(),
                reg.getActivity().getActivityName(),
                reg.getUser().getUserId(),
                reg.getUser().getName(),
                submission.getSubmissionUrl(),
                submission.getAwardedGems(),
                submission.getSubmittedAt(),
                submission.getReviewedAt(),
                submission.getReviewStatus()
        );
    }
}
