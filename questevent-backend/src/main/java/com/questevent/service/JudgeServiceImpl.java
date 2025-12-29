package com.questevent.service;

import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.Program;
import com.questevent.entity.User;
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
    public void reviewSubmission(Long submissionId, Integer awardedGems) {
        ActivitySubmission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() ->
                        new RuntimeException("Submission not found"));

        if (submission.getReviewedAt() != null) {
            throw new RuntimeException("Submission already reviewed");
        }


        if (awardedGems == null || awardedGems <= 0) {
            throw new IllegalArgumentException("Awarded gems must be greater than zero");
        }

        ActivityRegistration registration = submission.getActivityRegistration();
        User user = registration.getUser();
        Program program = registration.getActivity().getProgram();

        submission.setAwardedGems(awardedGems);
        submission.setReviewedAt(LocalDateTime.now());
        submissionRepository.save(submission);


        userWalletTransactionService.creditGems(user, awardedGems);

        programWalletTransactionService.creditGems(user, program, awardedGems);
    }
}
