package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(
        name = "Judge APIs",
        description = "Endpoints for judges to view, review, and manage activity submissions assigned to them"
)
public class JudgeController {

    private final JudgeService judgeService;


    @PreAuthorize("@rbac.canAccessJudgeSubmissions(authentication)")
    @GetMapping("/submissions")
    @Operation(
            summary = "Get all submissions for judge",
            description = "Returns all activity submissions (both pending and approved) "
                    + "for programs where the logged-in user is assigned as a judge. "
                    + "Platform owners can view submissions across all programs."
    )
    public ResponseEntity<List<JudgeSubmissionDTO>> getAllSubmissions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                judgeService.getAllSubmissionsForJudge(authentication)
        );
    }


    @PreAuthorize("@rbac.canAccessJudgeSubmissions(authentication)")
    @GetMapping("/submissions/pending")
    @Operation(
            summary = "Get pending submissions for judge",
            description = "Returns only PENDING activity submissions "
                    + "that are awaiting review by the logged-in judge. "
                    + "Platform owners can view all pending submissions."
    )
    public ResponseEntity<List<JudgeSubmissionDTO>> getPendingSubmissions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                judgeService.getPendingSubmissionsForJudge(authentication)
        );
    }


    @PreAuthorize("@rbac.isJudgeForActivity(authentication, #activityId)")
    @GetMapping("/submissions/activity/{activityId}")
    @Operation(
            summary = "Get submissions for a specific activity",
            description = "Returns all submissions (pending or reviewed) "
                    + "for a specific activity. "
                    + "Only the judge assigned to the activity's program "
                    + "or the platform owner can access this endpoint."
    )
    public ResponseEntity<List<JudgeSubmissionDTO>> getSubmissionsForActivity(
            @Parameter(
                    description = "ID of the activity whose submissions are to be fetched",
                    required = true,
                    example = "101"
            )
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(
                judgeService.getSubmissionsForActivity(activityId)
        );
    }


    @PreAuthorize("@rbac.canVerifySubmission(authentication, #submissionId)")
    @PostMapping("/review/{submissionId}")
    @Operation(
            summary = "Review and approve a submission",
            description = "Allows the assigned judge to review a submission, "
                    + "approve it, award gems to the participant, "
                    + "and mark the activity as completed. "
                    + "Only the assigned judge or platform owner can perform this action."
    )
    public ResponseEntity<String> reviewSubmission(
            @Parameter(
                    description = "Submission ID to be reviewed",
                    required = true,
                    example = "5001"
            )
            @PathVariable Long submissionId
    ) {
        judgeService.reviewSubmission(submissionId);
        return ResponseEntity.ok("Submission reviewed successfully");
    }
}
