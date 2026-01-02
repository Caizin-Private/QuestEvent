package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDto;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.JudgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<JudgeSubmissionDto> getSubmissionsForActivity(Long activityId) {

        return submissionRepository
                .findByActivityRegistrationActivityActivityId(activityId)
                .stream()
                .map(this::mapToJudgeSubmissionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeSubmissionDto> getPendingSubmissions() {

        return submissionRepository
                .findByActivityRegistrationCompletionStatus(
                        CompletionStatus.NOT_COMPLETED
                )
                .stream()
                .map(this::mapToJudgeSubmissionDto)
                .toList();
    }

    @Override
    @Transactional
    public void reviewSubmission(Long submissionId, Long judgeId) {

        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (submission.getReviewedAt() != null) {
            throw new RuntimeException("Submission already reviewed");
        }

        Judge judge = judgeRepository.findById(judgeId)
                .orElseThrow(() -> new RuntimeException("Judge not found"));

        ActivityRegistration registration = submission.getActivityRegistration();
        Activity activity = registration.getActivity();

        int rewardGems = activity.getRewardGems();
        if (rewardGems <= 0) {
            throw new RuntimeException("Invalid reward configuration");
        }

        // Update submission
        submission.setReviewedBy(judge);
        submission.setAwardedGems(rewardGems);
        submission.setReviewedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        // Update registration
        registration.setCompletionStatus(CompletionStatus.COMPLETED);
        registrationRepository.save(registration);

        // Credit program wallet
        User user = registration.getUser();
        Program program = activity.getProgram();

        programWalletTransactionService.creditGems(user, program, rewardGems);

    }


    private JudgeSubmissionDto mapToJudgeSubmissionDto(ActivitySubmission submission) {
        ActivityRegistration registration = submission.getActivityRegistration();

        return new JudgeSubmissionDto(
                submission.getSubmissionId(),
                registration.getActivity().getActivityId(),
                registration.getActivity().getActivityName(),
                registration.getUser().getUserId(),
                registration.getUser().getName(),
                submission.getSubmissionUrl(),
                submission.getAwardedGems(),
                submission.getSubmittedAt(),
                submission.getReviewedAt()
        );
    }

}
