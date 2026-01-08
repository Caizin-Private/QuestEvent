package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.JudgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeServiceImplTest {

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ProgramWalletTransactionService programWalletTransactionService;

    @InjectMocks
    private JudgeServiceImpl judgeService;

    @Test
    void getSubmissionsForActivity_shouldReturnMappedDtos() {

        UUID activityId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(activityId);

        when(submissionRepository
                .findByActivityRegistrationActivityActivityId(activityId))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getSubmissionsForActivity(activityId);

        assertEquals(1, result.size());
        assertEquals(activityId, result.get(0).activityId());
        assertEquals(ReviewStatus.PENDING, result.get(0).reviewStatus());
    }

    @Test
    void getPendingSubmissions_shouldReturnOnlyPending() {

        ActivitySubmission submission = mockSubmission(UUID.randomUUID());

        when(submissionRepository.findByReviewStatus(ReviewStatus.PENDING))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissions();

        assertEquals(1, result.size());
        assertEquals(ReviewStatus.PENDING, result.get(0).reviewStatus());
    }

    @Test
    void reviewSubmission_shouldApproveSubmissionAndCreditWallet() {

        UUID submissionId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(UUID.randomUUID());

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        judgeService.reviewSubmission(submissionId);

        assertEquals(ReviewStatus.APPROVED,
                submission.getReviewStatus());

        assertEquals(CompletionStatus.COMPLETED,
                submission.getActivityRegistration().getCompletionStatus());

        verify(programWalletTransactionService).creditGems(
                submission.getActivityRegistration().getUser(),
                submission.getActivityRegistration()
                        .getActivity()
                        .getProgram(),
                50L
        );
    }

    @Test
    void reviewSubmission_shouldThrowIfSubmissionNotFound() {

        UUID submissionId = UUID.randomUUID();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(submissionId));

        assertEquals("Submission not found", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfAlreadyReviewed() {

        UUID submissionId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(UUID.randomUUID());
        submission.setReviewStatus(ReviewStatus.APPROVED);

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(submissionId));

        assertEquals("Submission already reviewed", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfJudgeNotAssignedToProgram() {

        UUID submissionId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(UUID.randomUUID());
        submission.getActivityRegistration()
                .getActivity()
                .getProgram()
                .setJudge(null);

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(submissionId));

        assertEquals("Judge not assigned to this program", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfInvalidRewardGems() {

        UUID submissionId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(UUID.randomUUID());
        submission.getActivityRegistration()
                .getActivity()
                .setRewardGems(0L);

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(submissionId));

        assertEquals("Invalid reward configuration", ex.getMessage());
    }

    private ActivitySubmission mockSubmission(UUID activityId) {

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setName("User A");

        Judge judge = new Judge();
        judge.setJudgeId(UUID.randomUUID());

        Program program = new Program();
        program.setProgramId(UUID.randomUUID());
        program.setJudge(judge); // ✅ IMPORTANT

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Hackathon");
        activity.setRewardGems(50L);
        activity.setProgram(program);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivitySubmission submission = new ActivitySubmission();
        submission.setSubmissionId(UUID.randomUUID());
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl("http://link");
        submission.setSubmittedAt(Instant.now());
        submission.setReviewStatus(ReviewStatus.PENDING);

        return submission;
    }
}
