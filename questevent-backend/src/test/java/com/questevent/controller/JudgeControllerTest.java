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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeControllerTest {

    @Mock
    private JudgeService judgeService;

    @InjectMocks
    private JudgeController judgeController;

    @Test
    void getPendingSubmissions_shouldReturnList() {

        JudgeSubmissionDTO dto = new JudgeSubmissionDTO(
                UUID.randomUUID(),          // submissionId
                UUID.randomUUID(),          // activityId
                "Code Challenge",
                3L,                         // userId
                "Abhinash",
                "https://github.com/solution",
                null,
                Instant.now(),
                null,
                ReviewStatus.PENDING
        );

        when(judgeService.getPendingSubmissions())
                .thenReturn(List.of(dto));

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getPendingSubmissions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(ReviewStatus.PENDING, response.getBody().get(0).reviewStatus());
    }

    @Test
    void getSubmissionsForActivity_shouldReturnSubmissions() {

        UUID activityId = UUID.randomUUID();

        JudgeSubmissionDTO dto = new JudgeSubmissionDTO(
                UUID.randomUUID(),          // submissionId
                activityId,
                "Hackathon",
                4L,                         // userId
                "User A",
                "https://drive.link",
                null,
                Instant.now(),
                null,
                ReviewStatus.PENDING
        );

        when(judgeService.getSubmissionsForActivity(activityId))
                .thenReturn(List.of(dto));

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getSubmissionsForActivity(activityId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(activityId, response.getBody().get(0).activityId());
    }

    @Test
    void getSubmissionsForActivity_shouldThrowException_whenActivityNotFound() {

        UUID activityId = UUID.randomUUID();

        when(judgeService.getSubmissionsForActivity(activityId))
                .thenThrow(new RuntimeException("Activity not found"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> judgeController.getSubmissionsForActivity(activityId)
        );

        assertEquals("Activity not found", exception.getMessage());
    }

    @Test
    void reviewSubmission_shouldReturnSuccessMessage() {

        UUID submissionId = UUID.randomUUID();

        doNothing().when(judgeService)
                .reviewSubmission(submissionId);

        ResponseEntity<String> response =
                judgeController.reviewSubmission(submissionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Submission reviewed successfully", response.getBody());

        verify(judgeService, times(1))
                .reviewSubmission(submissionId);
    }

    @Test
    void reviewSubmission_shouldThrowException_whenAlreadyReviewed() {

        UUID submissionId = UUID.randomUUID();

        doThrow(new RuntimeException("Submission already reviewed"))
                .when(judgeService)
                .reviewSubmission(submissionId);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> judgeController.reviewSubmission(submissionId)
        );

        assertEquals("Submission already reviewed", exception.getMessage());
    }
}
