package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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

    static Stream<Arguments> submitActivityExceptionProvider() {
        return Stream.of(
                Arguments.of(
                        UUID.randomUUID(),
                        99L,
                        "https://github.com/user/project",
                        "User is not registered for this activity"
                ),
                Arguments.of(
                        UUID.randomUUID(),
                        100L,
                        "https://github.com/user/project",
                        "Submission already exists"
                ),
                Arguments.of(
                        UUID.randomUUID(),
                        101L,
                        "https://github.com/user/project",
                        "Activity is closed"
                )
        );
    }

    @Test
    void submitActivity_shouldReturnCreated_whenSubmissionIsSuccessful() {

        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setSubmissionUrl("https://github.com/user/project");

        doNothing().when(submissionService)
                .submitActivity(activityId, "https://github.com/user/project");

        ResponseEntity<String> response =
                submissionController.submitActivity(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Submission successful", response.getBody());
    }

    @ParameterizedTest
    @MethodSource("submitActivityExceptionProvider")
    void submitActivity_shouldThrowException(
            UUID activityId,
            Long userId,
            String submissionUrl,
            String expectedMessage
    ) {
        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException(expectedMessage))
                .when(submissionService)
                .submitActivity(activityId,"https://github.com/user/project");

        Executable executable =
                () -> submissionController.submitActivity(request);

        RuntimeException exception =
                assertThrows(RuntimeException.class, executable);

        assertEquals(expectedMessage, exception.getMessage());
    }


    @Test
    void submitActivity_shouldThrowException_whenSubmissionAlreadyExists() {

        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Submission already exists for this registration"))
                .when(submissionService)
                .submitActivity(activityId, "https://github.com/user/project");

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

        UUID activityId = UUID.randomUUID();
        Long userId = 2L;

        ActivitySubmissionRequestDTO request = new ActivitySubmissionRequestDTO();
        request.setActivityId(activityId);
        request.setSubmissionUrl("https://github.com/user/project");

        doThrow(new RuntimeException("Activity already completed. Submission not allowed."))
                .when(submissionService)
                .submitActivity(activityId, "https://github.com/user/project");

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
