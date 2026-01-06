package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;


    @Test
    void submitActivity_shouldReturnCreated_whenSubmissionIsSuccessful() {

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setUserId(2L);
        request.setSubmissionUrl("https://github.com/user/project");

        doNothing().when(submissionService)
                .submitActivity(1L, 2L, "https://github.com/user/project");

        ResponseEntity<String> response =
                submissionController.submitActivity(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Submission successful", response.getBody());
    }


    @Test
    void submitActivity_shouldThrowException_whenUserNotRegistered() {

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setUserId(99L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("User is not registered for this activity"))
                .when(submissionService)
                .submitActivity(1L, 99L, "https://github.com/user/project");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionController.submitActivity(request)
        );

        assertEquals(
                "User is not registered for this activity",
                exception.getMessage()
        );
    }


    @Test
    void submitActivity_shouldThrowException_whenSubmissionAlreadyExists() {

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setUserId(2L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Submission already exists for this registration"))
                .when(submissionService)
                .submitActivity(1L, 2L, "https://github.com/user/project");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionController.submitActivity(request)
        );

        assertEquals(
                "Submission already exists for this registration",
                exception.getMessage()
        );
    }


    @Test
    void submitActivity_shouldThrowException_whenActivityAlreadyCompleted() {

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setUserId(2L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Activity already completed. Submission not allowed."))
                .when(submissionService)
                .submitActivity(1L, 2L, "https://github.com/user/project");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionController.submitActivity(request)
        );

        assertEquals(
                "Activity already completed. Submission not allowed.",
                exception.getMessage()
        );
    }
}
