package com.questevent.service;

import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivitySubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ActivitySubmissionRepository submissionRepository;
    private final UserWalletTransactionService userWalletTransactionService;
    private final ProgramWalletTransactionService programWalletTransactionService;

    @Override
    public List<ActivitySubmission> getSubmissionsForActivity(Long activityId) {
        return submissionRepository
                .findByActivityRegistrationActivityActivityId(activityId);
    }

    @Override
    @Transactional
    public void reviewSubmission(Long submissionId) {
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (submission.getReviewedAt() != null) {
            throw new RuntimeException("Submission already reviewed");
        }

        ActivityRegistration registration = submission.getActivityRegistration();

        Activity activity = registration.getActivity();
        int rewardGems = activity.getRewardGems();

        if (rewardGems <= 0) {
            throw new RuntimeException("Invalid reward configuration");
        }

        submission.setAwardedGems(rewardGems);
        submission.setReviewedAt(LocalDateTime.now());

        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        submissionRepository.save(submission);

        User user = registration.getUser();
        Program program = activity.getProgram();

        programWalletTransactionService.creditGems(
                user,
                program,
                rewardGems
        );



    }
}
