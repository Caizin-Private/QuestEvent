package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.enums.ReviewStatus;
import com.questevent.service.JudgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeControllerTest {

    @Mock
    private JudgeService judgeService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JudgeController judgeController;

    @Test
    void getPendingSubmissions_shouldReturnList() {

        JudgeSubmissionDTO dto = new JudgeSubmissionDTO(
                1L,
                10L,
                "Code Challenge",
                3L,
                "Abhinash",
                "https://github.com/solution",
                null,
                Instant.now(),
                null,
                ReviewStatus.PENDING
        );

        when(judgeService.getPendingSubmissionsForJudge(authentication))
                .thenReturn(List.of(dto));

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getPendingSubmissions(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(ReviewStatus.PENDING, response.getBody().get(0).reviewStatus());
    }

    @Test
    void getPendingSubmissionsForActivity_shouldReturnSubmissions() {

        Long activityId = 10L;

        JudgeSubmissionDTO dto = new JudgeSubmissionDTO(
                2L,
                activityId,
                "Hackathon",
                4L,
                "User A",
                "https://drive.link",
                null,
                Instant.now(),
                null,
                ReviewStatus.PENDING
        );

        when(judgeService.getPendingSubmissionsForActivity(activityId, authentication))
                .thenReturn(List.of(dto));

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getPendingSubmissionsForActivity(activityId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(activityId, response.getBody().get(0).activityId());
    }

    @Test
    void getPendingSubmissionsForActivity_shouldThrowException_whenActivityNotFound() {

        Long activityId = 999L;

        when(judgeService.getPendingSubmissionsForActivity(activityId, authentication))
                .thenThrow(new RuntimeException("Activity not found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> judgeController.getPendingSubmissionsForActivity(activityId, authentication)
        );

        assertEquals("Activity not found", exception.getMessage());
    }

    @Test
    void getAllSubmissions_shouldReturnList() {

        when(judgeService.getAllSubmissionsForJudge(authentication))
                .thenReturn(List.of());

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getAllSubmissions(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void reviewSubmission_shouldReturnOk() {

        Long submissionId = 1L;

        doNothing().when(judgeService)
                .reviewSubmission(submissionId);

        ResponseEntity<Void> response =
                judgeController.reviewSubmission(submissionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(judgeService, times(1))
                .reviewSubmission(submissionId);
    }

    @Test
    void reviewSubmission_shouldThrowException_whenAlreadyReviewed() {

        doThrow(new RuntimeException("Submission already reviewed"))
                .when(judgeService)
                .reviewSubmission(1L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> judgeController.reviewSubmission(1L)
        );

        assertEquals("Submission already reviewed", exception.getMessage());
    }
}
