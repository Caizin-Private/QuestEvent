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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JudgeServiceImpl judgeService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /* ================= PENDING ================= */

    @Test
    void getPendingSubmissionsForJudge_shouldReturnPendingForJudge() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);

        mockAuthenticatedUser(judgeUser);

        when(userRepository.findById(judgeUser.getUserId()))
                .thenReturn(Optional.of(judgeUser));

        when(submissionRepository
                .findByReviewStatusAndActivityRegistrationActivityProgramJudgeUserUserId(
                        ReviewStatus.PENDING,
                        judgeUser.getUserId()
                ))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissionsForJudge(null);

        assertEquals(1, result.size());
        assertEquals(ReviewStatus.PENDING, result.get(0).reviewStatus());
    }

    /* ================= REVIEW ================= */

    @Test
    void reviewSubmission_shouldApproveSubmissionAndCreditWallet() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);


        mockAuthenticatedUser(judgeUser);


        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        judgeService.reviewSubmission(10L);

        assertEquals(
                ReviewStatus.APPROVED,
                submission.getReviewStatus()
        );

        assertEquals(
                CompletionStatus.COMPLETED,
                submission.getActivityRegistration().getCompletionStatus()
        );

        verify(programWalletTransactionService).creditGems(
                submission.getActivityRegistration().getUser(),
                submission.getActivityRegistration()
                        .getActivity()
                        .getProgram(),
                50
        );
    }

    @Test
    void reviewSubmission_shouldThrowIfSubmissionNotFound() {

        mockAuthenticatedUser(mockJudgeUser());

        when(submissionRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> judgeService.reviewSubmission(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Submission not found", ex.getReason());
    }

    @Test
    void reviewSubmission_shouldThrowIfAlreadyReviewed() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);
        submission.setReviewStatus(ReviewStatus.APPROVED);

        mockAuthenticatedUser(judgeUser);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> judgeService.reviewSubmission(10L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Submission already reviewed", ex.getReason());
    }

    @Test
    void reviewSubmission_shouldThrowIfJudgeNotAssigned() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);
        submission.getActivityRegistration()
                .getActivity()
                .getProgram()
                .setJudge(null);

        mockAuthenticatedUser(judgeUser);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> judgeService.reviewSubmission(10L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Judge not assigned to this program", ex.getReason());
    }

    @Test
    void reviewSubmission_shouldThrowIfInvalidRewardGems() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);
        submission.getActivityRegistration()
                .getActivity()
                .setRewardGems(-1);

        mockAuthenticatedUser(judgeUser);

        when(submissionRepository.findById(10L))
                .thenReturn(Optional.of(submission));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> judgeService.reviewSubmission(10L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid reward gems", ex.getReason());
    }

    /* ================= HELPERS ================= */

    private void mockAuthenticatedUser(User user) {
        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), "judge@test.com", user.getRole());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                )
        );
    }

    private User mockJudgeUser() {
        User user = new User();
        user.setUserId(5L);
        user.setRole(Role.JUDGE);
        return user;
    }

    private ActivitySubmission mockSubmission(User judgeUser) {

        User participant = new User();
        participant.setUserId(20L);
        participant.setName("Participant");

        Judge judge = new Judge();
        judge.setJudgeId(1L);
        judge.setUser(judgeUser);

        Program program = new Program();
        program.setProgramId(3L);
        program.setJudge(judge);

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Hackathon");
        activity.setRewardGems(50);
        activity.setProgram(program);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(participant);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivitySubmission submission = new ActivitySubmission();
        submission.setSubmissionId(10L);
        submission.setActivityRegistration(registration);
        submission.setSubmissionUrl("http://link");
        submission.setSubmittedAt(Instant.now());
        submission.setReviewStatus(ReviewStatus.PENDING);

        return submission;
    }
}
