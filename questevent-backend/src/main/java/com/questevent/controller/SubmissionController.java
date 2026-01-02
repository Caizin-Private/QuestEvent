package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Activity Submissions", description = "APIs for submitting activity work")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @Operation(
            summary = "Submit activity work",
            description = "Allows a registered user to submit work for an activity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Submission successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request or submission already exists"),
            @ApiResponse(responseCode = "403", description = "Activity already completed, submission not allowed"),
            @ApiResponse(responseCode = "404", description = "User is not registered for the activity")
    })
    public ResponseEntity<String> submitActivity(
            @RequestBody ActivitySubmissionRequestDTO request
    ) {
        submissionService.submitActivity(
                request.getActivityId(),
                request.getUserId(),
                request.getSubmissionUrl()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Submission successful");
    }
}