package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDTO;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Activity Submissions", description = "APIs for submitting activity work")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    private final SubmissionService submissionService;

    @PreAuthorize("@rbac.canSubmitActivity(authentication, #request.activityId, authentication.principal.userId)")
    @PostMapping
    @Operation(
            summary = "Submit activity work",
            description = "Allows a registered user to submit work for an activity"
    )
    public ResponseEntity<String> submitActivity(
            @RequestBody ActivitySubmissionRequestDTO request
    ) {
        log.info("Submitting activity work: activityId={}, userId={}",
                request.getActivityId(), request.getUserId());

        submissionService.submitActivity(
                request.getActivityId(),
                request.getUserId(),
                request.getSubmissionUrl()
        );

        log.info("Activity submission successful: activityId={}, userId={}",
                request.getActivityId(), request.getUserId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Submission successful");
    }
}
