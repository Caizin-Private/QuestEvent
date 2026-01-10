package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @MethodSource("submitActivityFailureScenarios")
    void submitActivity_shouldThrowException_forInvalidCases(
            Long userId,
            String errorMessage
    ) {
        UUID activityId = UUID.randomUUID();

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setUserId(userId);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException(errorMessage))
                .when(submissionService)
                .submitActivity(activityId, userId, "https://github.com/user/project");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionController.submitActivity(request)
        );

        assertEquals(errorMessage, exception.getMessage());
    }

    private static Stream<Arguments> submitActivityFailureScenarios() {
        return Stream.of(
                Arguments.of(
                        99L,
                        "User is not registered for this activity"
                ),
                Arguments.of(
                        2L,
                        "Submission already exists for this registration"
                ),
                Arguments.of(
                        2L,
                        "Activity already completed. Submission not allowed."
                )
        );
    }
}
