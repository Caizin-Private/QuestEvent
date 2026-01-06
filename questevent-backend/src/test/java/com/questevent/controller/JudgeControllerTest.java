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

import java.time.LocalDateTime;
import java.util.List;

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
                1L,
                10L,
                "Code Challenge",
                3L,
                "Abhinash",
                "https://github.com/solution",
                null,
                LocalDateTime.now(),
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

        Long activityId = 10L;

        JudgeSubmissionDTO dto = new JudgeSubmissionDTO(
                2L,
                activityId,
                "Hackathon",
                4L,
                "User A",
                "https://drive.link",
                null,
                LocalDateTime.now(),
                null,
                ReviewStatus.PENDING
        );

        when(judgeService.getSubmissionsForActivity(activityId))
                .thenReturn(List.of(dto));

        ResponseEntity<List<JudgeSubmissionDTO>> response =
                judgeController.getSubmissionsForActivity(activityId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(activityId, response.getBody().get(0).activityId());
    }


    @Test
    void getSubmissionsForActivity_shouldThrowException_whenActivityNotFound() {

        Long activityId = 999L;

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

        Long submissionId = 1L;
        Long judgeId = 2L;

        doNothing().when(judgeService)
                .reviewSubmission(submissionId, judgeId);

        ResponseEntity<String> response =
                judgeController.reviewSubmission(submissionId, judgeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Submission reviewed successfully", response.getBody());
    }


    @Test
    void reviewSubmission_shouldThrowException_whenAlreadyReviewed() {

        doThrow(new RuntimeException("Submission already reviewed"))
                .when(judgeService)
                .reviewSubmission(1L, 1L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> judgeController.reviewSubmission(1L, 1L)
        );

        assertEquals("Submission already reviewed", exception.getMessage());
    }
}