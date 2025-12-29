package com.questevent.controller;

import com.questevent.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<String> submitActivity(
            @RequestParam Long activityId,
            @RequestParam Long userId,
            @RequestParam String submissionUrl
    ) {
        submissionService.submitActivity(activityId, userId, submissionUrl);
        return ResponseEntity.ok("Submission successful");
    }

}