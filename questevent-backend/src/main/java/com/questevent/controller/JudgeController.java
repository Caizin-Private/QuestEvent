package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
@Tag(name = "Judge", description = "APIs for judges to review and manage submissions")
public class JudgeController {

    private final JudgeService judgeService;


    @PreAuthorize("@rbac.canAccessJudgeSubmissions(authentication)")
    @GetMapping("/submissions")
    @Operation(summary = "Get all submissions for judge")
    public ResponseEntity<List<JudgeSubmissionDTO>> getAllSubmissions(Authentication authentication)
    {
        return ResponseEntity.ok(
                judgeService.getAllSubmissionsForJudge(authentication)
        );
    }


    @PreAuthorize("@rbac.canAccessJudgeSubmissions(authentication)")
    @GetMapping("/submissions/pending")
    @Operation(summary = "Get pending submissions for judge")
    public ResponseEntity<List<JudgeSubmissionDTO>> getPendingSubmissions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                judgeService.getPendingSubmissionsForJudge(authentication)
        );
    }


    @PreAuthorize("@rbac.isJudgeForActivity(authentication, #activityId)")
    @GetMapping("/submissions/activity/{activityId}")
    public ResponseEntity<List<JudgeSubmissionDTO>> getSubmissionsForActivity(
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(
                judgeService.getSubmissionsForActivity(activityId)
        );
    }


    @PreAuthorize("@rbac.canVerifySubmission(authentication, #submissionId)")
    @PostMapping("/review/{submissionId}")
    public ResponseEntity<String> reviewSubmission(
            @PathVariable Long submissionId
    ) {
        judgeService.reviewSubmission(submissionId);
        return ResponseEntity.ok("Submission reviewed successfully");
    }
}
