package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.rbac.RbacService;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final ProgramWalletTransactionService programWalletTransactionService;
    private final RbacService rbacService;

    // ✅ ALL submissions
    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getAllSubmissionsForJudge(
            Authentication authentication
    ) {
        User user = rbacService.currentUser(authentication);

        if (user == null) return List.of();

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

    // ✅ ONLY pending
    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissionsForJudge(
            Authentication authentication
    ) {
        User user = rbacService.currentUser(authentication);

        if (user == null) return List.of();

        if (user.getRole() == Role.OWNER) {
            return submissionRepository.findByReviewStatus(ReviewStatus.PENDING)
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

    // ✅ By activity
    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getSubmissionsForActivity(Long activityId) {
        return submissionRepository
                .findByActivityRegistrationActivityActivityId(activityId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // ✅ Review
    @Override
    @Transactional
    public void reviewSubmission(Long submissionId) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new RuntimeException("Submission already reviewed");
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        Activity activity = registration.getActivity();
        Program program = activity.getProgram();

        Judge judge = program.getJudge();
        if (judge == null) {
            throw new RuntimeException("Judge not assigned");
        }

        int rewardGems = activity.getRewardGems();

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
