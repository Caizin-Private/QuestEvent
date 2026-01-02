package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDto;
import com.questevent.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PreAuthorize("@rbac.canSubmitActivity(authentication, #request.activityId, #request.userId)")
    @PostMapping
    public ResponseEntity<String> submitActivity(
            @RequestBody ActivitySubmissionRequestDto request
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