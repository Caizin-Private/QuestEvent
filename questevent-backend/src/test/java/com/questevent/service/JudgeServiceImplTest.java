package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // ---------------- READ APIs ----------------

    @Test
    void getSubmissionsForActivity_shouldReturnMappedDtos() {
        ActivitySubmission submission = mockSubmission();

        when(submissionRepository.findByActivityRegistrationActivityActivityId(1L))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getSubmissionsForActivity(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).activityId());
        assertEquals(ReviewStatus.PENDING, result.get(0).reviewStatus());
    }

    @Test
    void getPendingSubmissions_shouldReturnOnlyPending() {
        ActivitySubmission submission = mockSubmission();

        when(submissionRepository.findByReviewStatus(ReviewStatus.PENDING))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissions();

        assertEquals(1, result.size());
        assertEquals(ReviewStatus.PENDING, result.get(0).reviewStatus());
    }

    // ---------------- REVIEW API ----------------

    @Test
    void reviewSubmission_shouldApproveSubmissionAndCreditWallet() {
        ActivitySubmission submission = mockSubmission();

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        judgeService.reviewSubmission(10L);

        assertEquals(ReviewStatus.APPROVED, submission.getReviewStatus());
        assertEquals(CompletionStatus.COMPLETED,
                submission.getActivityRegistration().getCompletionStatus());

        verify(programWalletTransactionService).creditGems(
                submission.getActivityRegistration().getUser(),
                submission.getActivityRegistration().getActivity().getProgram(),
                50
        );
    }

    @Test
    void reviewSubmission_shouldThrowIfSubmissionNotFound() {
        when(submissionRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(99L));

        assertEquals("Submission not found", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfAlreadyReviewed() {
        ActivitySubmission submission = mockSubmission();
        submission.setReviewStatus(ReviewStatus.APPROVED);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(10L));

        assertEquals("Submission already reviewed", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfJudgeNotAssigned() {
        ActivitySubmission submission = mockSubmission();
        submission.getActivityRegistration()
                .getActivity()
                .getProgram()
                .setJudge(null);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(10L));

        assertEquals("Judge not assigned to this program", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfInvalidRewardGems() {
        ActivitySubmission submission = mockSubmission();
        submission.getActivityRegistration()
                .getActivity()
                .setRewardGems(0);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> judgeService.reviewSubmission(10L));

        assertEquals("Invalid reward configuration", ex.getMessage());
    }

    // ---------------- MOCK GRAPH ----------------

    private ActivitySubmission mockSubmission() {
        User user = new User();
        user.setUserId(5L);
        user.setName("User A");

        Judge judge = new Judge();
        judge.setJudgeId(1L);

        Program program = new Program();
        program.setProgramId(3L);
        program.setJudge(judge); // 🔑 CRITICAL

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Hackathon");
        activity.setRewardGems(50);
        activity.setProgram(program);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivitySubmission submission = new ActivitySubmission();
        submission.setSubmissionId(10L);
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl("http://link");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setReviewStatus(ReviewStatus.PENDING);

        return submission;
    }
}
