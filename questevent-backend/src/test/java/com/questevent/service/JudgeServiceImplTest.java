package com.questevent.service;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.exception.*;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Test
    void getPendingSubmissions_shouldThrowUnauthorized_whenNotAuthenticated() {

        SecurityContextHolder.clearContext();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> judgeService.getPendingSubmissionsForJudge(null)
        );

        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void getPendingSubmissions_shouldThrowUserNotFound_whenUserMissing() {

        UserPrincipal principal =
                new UserPrincipal(99L, "missing@test.com", Role.JUDGE);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> judgeService.getPendingSubmissionsForJudge(null)
        );

        assertEquals("User not found", ex.getMessage());
    }


    @Test
    void getPendingSubmissionsForJudge_shouldReturnAllPending_whenOwner() {

        User owner = new User();
        owner.setUserId(1L);
        owner.setRole(Role.OWNER);

        ActivitySubmission submission = mockSubmission(owner);

        mockAuthenticatedUser(owner);

        when(userRepository.findById(owner.getUserId()))
                .thenReturn(Optional.of(owner));

        when(submissionRepository.findByReviewStatus(ReviewStatus.PENDING))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissionsForJudge(null);

        assertEquals(1, result.size());
    }

    @Test
    void getPendingSubmissionsForActivity_shouldReturnAllPending_whenOwner() {

        User owner = new User();
        owner.setUserId(1L);
        owner.setRole(Role.OWNER);

        UUID activityId = UUID.randomUUID();
        ActivitySubmission submission = mockSubmission(owner);

        mockAuthenticatedUser(owner);

        when(userRepository.findById(owner.getUserId()))
                .thenReturn(Optional.of(owner));

        when(submissionRepository
                .findByReviewStatusAndActivityRegistrationActivityActivityId(
                        ReviewStatus.PENDING,
                        activityId))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissionsForActivity(activityId, null);

        assertEquals(1, result.size());
    }

    @Test
    void getPendingSubmissionsForActivity_shouldReturnJudgeSubmissions() {

        User judgeUser = mockJudgeUser();
        UUID activityId = UUID.randomUUID();

        ActivitySubmission submission = mockSubmission(judgeUser);

        mockAuthenticatedUser(judgeUser);

        when(userRepository.findById(judgeUser.getUserId()))
                .thenReturn(Optional.of(judgeUser));

        when(submissionRepository
                .findByReviewStatusAndActivityRegistrationActivityActivityIdAndActivityRegistrationActivityProgramJudgeUserUserId(
                        ReviewStatus.PENDING,
                        activityId,
                        judgeUser.getUserId()))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getPendingSubmissionsForActivity(activityId, null);

        assertEquals(1, result.size());
    }


    @Test
    void getAllSubmissionsForJudge_shouldReturnAll_whenOwner() {

        User owner = new User();
        owner.setUserId(1L);
        owner.setRole(Role.OWNER);

        ActivitySubmission submission = mockSubmission(owner);

        mockAuthenticatedUser(owner);

        when(userRepository.findById(owner.getUserId()))
                .thenReturn(Optional.of(owner));

        when(submissionRepository.findAll())
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getAllSubmissionsForJudge(null);

        assertEquals(1, result.size());
    }

    @Test
    void getAllSubmissionsForJudge_shouldReturnJudgeSubmissions() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);

        mockAuthenticatedUser(judgeUser);

        when(userRepository.findById(judgeUser.getUserId()))
                .thenReturn(Optional.of(judgeUser));

        when(submissionRepository
                .findByActivityRegistrationActivityProgramJudgeUserUserId(
                        judgeUser.getUserId()))
                .thenReturn(List.of(submission));

        List<JudgeSubmissionDTO> result =
                judgeService.getAllSubmissionsForJudge(null);

        assertEquals(1, result.size());
    }











    /* ================= REVIEW ================= */

    @Test
    void reviewSubmission_shouldApproveSubmissionAndCreditWallet() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);

        mockAuthenticatedUser(judgeUser);

        UUID submissionId = submission.getSubmissionId();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        judgeService.reviewSubmission(submissionId);

        assertEquals(ReviewStatus.APPROVED, submission.getReviewStatus());
        assertEquals(
                CompletionStatus.COMPLETED,
                submission.getActivityRegistration().getCompletionStatus()
        );

        verify(programWalletTransactionService).creditGems(
                submission.getActivityRegistration().getUser(),
                submission.getActivityRegistration().getActivity().getProgram(),
                50L
        );
    }

    @Test
    void reviewSubmission_shouldThrowIfSubmissionNotFound() {

        mockAuthenticatedUser(mockJudgeUser());

        UUID submissionId = UUID.randomUUID();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.empty());

        SubmissionNotFoundException ex = assertThrows(
                SubmissionNotFoundException.class,
                () -> judgeService.reviewSubmission(submissionId)
        );

        assertEquals("Submission not found", ex.getMessage());
    }

    @Test
    void reviewSubmission_shouldThrowIfAlreadyReviewed() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);
        submission.setReviewStatus(ReviewStatus.APPROVED);

        mockAuthenticatedUser(judgeUser);

        UUID submissionId = submission.getSubmissionId();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> judgeService.reviewSubmission(submissionId)
        );

        assertEquals("Submission already reviewed", ex.getMessage());
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

        UUID submissionId = submission.getSubmissionId();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        JudgeNotFoundException ex = assertThrows(
                JudgeNotFoundException.class,
                () -> judgeService.reviewSubmission(submissionId)
        );

        assertEquals(
                "Judge not assigned to this program",
                ex.getMessage()
        );
    }

    @Test
    void reviewSubmission_shouldThrowIfInvalidRewardGems() {

        User judgeUser = mockJudgeUser();
        ActivitySubmission submission = mockSubmission(judgeUser);

        submission.getActivityRegistration()
                .getActivity()
                .setRewardGems(-1L);

        mockAuthenticatedUser(judgeUser);

        UUID submissionId = submission.getSubmissionId();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));

        InvalidOperationException ex = assertThrows(
                InvalidOperationException.class,
                () -> judgeService.reviewSubmission(submissionId)
        );

        assertEquals("Invalid reward gems", ex.getMessage());
    }




    private void mockAuthenticatedUser(User user) {
        UserPrincipal principal =
                new UserPrincipal(user.getUserId(), "judge@test.com", user.getRole());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
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
        judge.setJudgeId(UUID.randomUUID());
        judge.setUser(judgeUser);

        Program program = new Program();
        program.setProgramId(UUID.randomUUID());
        program.setJudge(judge);

        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Hackathon");
        activity.setRewardGems(50L);
        activity.setProgram(program);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(UUID.randomUUID());
        registration.setActivity(activity);
        registration.setUser(participant);
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
