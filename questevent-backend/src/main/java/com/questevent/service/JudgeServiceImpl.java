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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final ProgramWalletTransactionService programWalletTransactionService;
    private final UserRepository userRepository;

    /* ================= USER RESOLUTION ================= */

    private User resolveCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal p) {
            return userRepository.findById(p.userId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "User not found"));
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid authentication");
    }

    /* ================= PENDING (JUDGE-SCOPED) ================= */

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        if (user.getRole() == Role.OWNER) {
            return submissionRepository
                    .findByReviewStatus(ReviewStatus.PENDING)
                    .stream()
                    .map(this::mapToDto)
                    .toList();
        }

        return submissionRepository
                .findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
                        ReviewStatus.PENDING,
                        user.getUserId()
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /* ================= PENDING BY ACTIVITY ================= */

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForActivity(
            Long activityId,
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        if (user.getRole() == Role.OWNER) {
            return submissionRepository
                    .findByReviewStatusAndActivityRegistrationActivityActivityId(
                            ReviewStatus.PENDING,
                            activityId
                    )
                    .stream()
                    .map(this::mapToDto)
                    .toList();
        }

        return submissionRepository
                .findByReviewStatusAndActivityRegistrationActivityActivityIdAndActivityRegistrationActivityProgramJudgeUserUserId(
                        ReviewStatus.PENDING,
                        activityId,
                        user.getUserId()
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /* ================= ALL SUBMISSIONS ================= */

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = resolveCurrentUser();

        if (user.getRole() == Role.OWNER) {
            return submissionRepository.findAll()
                    .stream()
                    .map(this::mapToDto)
                    .toList();
        }

        return submissionRepository
                .findByActivityRegistrationActivityProgramJudgeUserUserId(
                        user.getUserId()
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /* ================= REVIEW ================= */

    @Override
    @Transactional
    public void reviewSubmission(Long submissionId) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Submission not found"));

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Submission already reviewed");
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        Activity activity = registration.getActivity();
        Program program = activity.getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Judge not assigned to this program");
        }

        Integer rewardGems = activity.getRewardGems();
        if (rewardGems == null || rewardGems < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reward gems");
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

    /* ================= MAPPER ================= */

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