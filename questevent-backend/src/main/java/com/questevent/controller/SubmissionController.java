package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
import com.questevent.dto.SubmissionDetailsResponseDTO;
import com.questevent.dto.SubmissionStatusResponseDTO;
import com.questevent.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.questevent.service.SubmissionQueryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Activity Submissions", description = "APIs for submitting activity work")
public class SubmissionController {

    private static final Logger log =
            LoggerFactory.getLogger(SubmissionController.class);

    private final SubmissionService submissionService;
    private final SubmissionQueryService submissionQueryService;

    @PreAuthorize("@rbac.canSubmitActivity(authentication, #request.activityId, authentication.principal.userId)")
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
        log.info("Submitting activity work: activityId={}, userId={}",
                request.getActivityId());

        submissionService.submitActivity(
                request.getActivityId(),
                request.getSubmissionUrl()
        );

        log.info("Activity submission successful: activityId={}, userId={}",
                request.getActivityId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Submission successful");
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{activityId}/status")
    @Operation(
            summary = "Get submission status",
            description = "Allows a user to check whether their submission is pending, approved, or rejected"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission status fetched"),
            @ApiResponse(responseCode = "404", description = "Submission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<SubmissionStatusResponseDTO> getSubmissionStatus(
            @PathVariable UUID activityId
    ) {
        log.info("Fetching submission status: activityId={}", activityId);

        return ResponseEntity.ok(
                submissionQueryService.getSubmissionStatus(activityId)
        );
    }




    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{activityId}")
    @Operation(
            summary = "Get submission details",
            description = "Fetch the logged-in user's submission details for an activity"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission details fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Submission not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<SubmissionDetailsResponseDTO> getSubmissionDetails(
            @PathVariable UUID activityId,
            Authentication authentication
    ) {
        log.info("Fetching submission details | activityId={}", activityId);

        SubmissionDetailsResponseDTO response =
                submissionQueryService.getSubmissionDetails(activityId, authentication);

        return ResponseEntity.ok(response);
    }
}
