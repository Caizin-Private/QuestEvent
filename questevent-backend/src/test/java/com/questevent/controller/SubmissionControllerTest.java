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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;

    @Test
    void submitActivity_shouldReturnCreated_whenSubmissionIsSuccessful() {

        // Arrange
        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setSubmissionUrl("https://github.com/user/project");

        doNothing()
                .when(submissionService)
                .submitActivity(1L, "https://github.com/user/project");

        // Act
        ResponseEntity<String> response =
                submissionController.submitActivity(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Submission successful", response.getBody());

        verify(submissionService)
                .submitActivity(1L, "https://github.com/user/project");
    }

    @Test
    void submitActivity_shouldThrowException_whenUserNotRegistered() {

        // Arrange
        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("User is not registered for this activity"))
                .when(submissionService)
                .submitActivity(1L, "https://github.com/user/project");

        // Act + Assert
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

        // Arrange
        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Submission already exists for this registration"))
                .when(submissionService)
                .submitActivity(1L, "https://github.com/user/project");

        // Act + Assert
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

        // Arrange
        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(1L);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Activity already completed. Submission not allowed."))
                .when(submissionService)
                .submitActivity(1L, "https://github.com/user/project");

        // Act + Assert
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
