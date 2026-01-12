package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.stream.Stream;

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

        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setUserId(userId);
        request.setSubmissionUrl("https://github.com/user/project");

        doNothing().when(submissionService)
                .submitActivity(activityId, userId, "https://github.com/user/project");

        ResponseEntity<String> response =
                submissionController.submitActivity(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Submission successful", response.getBody());
    }

    @ParameterizedTest
    @MethodSource("exceptionScenarios")
    void submitActivity_shouldThrowException_forInvalidCases(
            RuntimeException thrownException,
            String expectedMessage
    ) {
        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setUserId(userId);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(thrownException)
                .when(submissionService)
                .submitActivity(activityId, userId, "https://github.com/user/project");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionController.submitActivity(request)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> exceptionScenarios() {
        return Stream.of(
                Arguments.of(
                        new RuntimeException("User is not registered for this activity"),
                        "User is not registered for this activity"
                ),
                Arguments.of(
                        new RuntimeException("Submission already exists for this registration"),
                        "Submission already exists for this registration"
                ),
                Arguments.of(
                        new RuntimeException("Activity already completed. Submission not allowed."),
                        "Activity already completed. Submission not allowed."
                )
        );
    }
}
