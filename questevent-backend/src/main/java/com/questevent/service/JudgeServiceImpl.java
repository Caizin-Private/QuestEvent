package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.JudgeSubmissionDetailsDTO;
import com.questevent.dto.JudgeSubmissionStatsDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.exception.*;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SecurityUserResolver securityUserResolver; // ✅ added

    private User currentUser() {
        return securityUserResolver.getCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = currentUser();

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

    @Override
    @Transactional
    public void rejectSubmission(
            UUID submissionId,
            Authentication ignored
    ) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() ->
                        new SubmissionNotFoundException("Submission not found")
                );

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new InvalidSubmissionStateException(
                    "Submission already reviewed"
            );
        }

        validateJudgeAssignment(submission);

        submission.setReviewStatus(ReviewStatus.REJECTED);
        submission.setReviewedAt(Instant.now());

        ActivityRegistration registration =
                submission.getActivityRegistration();

        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);
        registrationRepository.save(registration);
        submissionRepository.save(submission);
    }

    private void validateJudgeAssignment(ActivitySubmission submission) {

        User user = currentUser();

        Program program = submission
                .getActivityRegistration()
                .getActivity()
                .getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new JudgeNotFoundException(
                    "Judge not assigned to this program"
            );
        }

        if (!judge.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException(
                    "Only the assigned judge can reject this submission"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JudgeSubmissionDetailsDTO getSubmissionDetails(
            UUID submissionId,
            Authentication ignored
    ) {
        User user = currentUser();

        ActivitySubmission submission =
                submissionRepository.findById(submissionId)
                        .orElseThrow(() ->
                                new SubmissionNotFoundException(
                                        "Submission not found"
                                )
                        );

        Program program =
                submission.getActivityRegistration()
                        .getActivity()
                        .getProgram();

        Judge judge = program.getJudge();
        if (judge == null ||
                !judge.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException(
                    "Only the assigned judge can view this submission"
            );
        }

        return new JudgeSubmissionDetailsDTO(
                submission.getSubmissionId(),
                submission.getActivityRegistration().getActivity().getActivityId(),
                submission.getActivityRegistration().getActivity().getActivityName(),
                submission.getActivityRegistration().getUser().getUserId(),
                submission.getActivityRegistration().getUser().getEmail(),
                submission.getSubmissionUrl(),
                submission.getReviewStatus(),
                submission.getReviewedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public JudgeSubmissionStatsDTO getSubmissionStats(
            Authentication ignored
    ) {
        User user = currentUser();
        Long judgeUserId = user.getUserId();


        long pending =
                submissionRepository
                        .countByReviewStatusAndActivityRegistration_Activity_Program_Judge_User_UserId(
                                ReviewStatus.PENDING,
                                judgeUserId
                        );

        long approved =
                submissionRepository
                        .countByReviewStatusAndActivityRegistration_Activity_Program_Judge_User_UserId(
                                ReviewStatus.APPROVED,
                                judgeUserId
                        );

        long rejected =
                submissionRepository
                        .countByReviewStatusAndActivityRegistration_Activity_Program_Judge_User_UserId(
                                ReviewStatus.REJECTED,
                                judgeUserId
                        );

        return new JudgeSubmissionStatsDTO(
                pending,
                approved,
                rejected
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForActivity(
            UUID activityId,
            Authentication ignored
    ) {
        User user = currentUser();

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

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication ignored
    ) {
        User user = currentUser();

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

    @Override
    @Transactional
    public void reviewSubmission(UUID submissionId) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() ->
                        new SubmissionNotFoundException("Submission not found")
                );

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new InvalidOperationException(
                    "Submission already reviewed"
            );
        }

        ActivityRegistration registration =
                submission.getActivityRegistration();

        Activity activity = registration.getActivity();
        Program program = activity.getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new JudgeNotFoundException(
                    "Judge not assigned to this program"
            );
        }

        Long rewardGems = activity.getRewardGems();
        if (rewardGems == null || rewardGems < 0) {
            throw new InvalidOperationException("Invalid reward gems");
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

    private JudgeSubmissionDTO mapToDto(ActivitySubmission submission) {

        ActivityRegistration reg =
                submission.getActivityRegistration();

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
