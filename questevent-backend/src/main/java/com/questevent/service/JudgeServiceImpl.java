package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.JudgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ActivitySubmissionRepository submissionRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final JudgeRepository judgeRepository;
    private final ProgramWalletTransactionService programWalletTransactionService;

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getSubmissionsForActivity(Long activityId) {

        return submissionRepository
                .findByActivityRegistrationActivityActivityId(activityId)
                .stream()
                .map(this::mapToJudgeSubmissionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDTO> getPendingSubmissions() {

        return submissionRepository
                .findByReviewStatus(ReviewStatus.PENDING)
                .stream()
                .map(this::mapToJudgeSubmissionDto)
                .toList();
    }

    @Override
    @Transactional
    public void reviewSubmission(Long submissionId) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Submission not found")
                );

        if (submission.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("Submission already reviewed");
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        Activity activity = registration.getActivity();
        Program program = activity.getProgram();

        // Judge assigned at program creation
        Judge judge = program.getJudge();
        if (judge == null) {
            throw new IllegalStateException("Judge not assigned to this program");
        }

        int rewardGems = activity.getRewardGems();
        if (rewardGems <= 0) {
            throw new IllegalStateException("Invalid reward configuration");
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

    private JudgeSubmissionDTO mapToJudgeSubmissionDto(ActivitySubmission submission) {

        ActivityRegistration registration = submission.getActivityRegistration();

        return new JudgeSubmissionDTO(
                submission.getSubmissionId(),
                registration.getActivity().getActivityId(),
                registration.getActivity().getActivityName(),
                registration.getUser().getUserId(),
                registration.getUser().getName(),
                submission.getSubmissionUrl(),
                submission.getAwardedGems(),
                submission.getSubmittedAt(),
                submission.getReviewedAt(),
                submission.getReviewStatus()
        );
    }
}