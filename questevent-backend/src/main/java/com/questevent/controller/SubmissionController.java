package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionRequestDto;
import com.questevent.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

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