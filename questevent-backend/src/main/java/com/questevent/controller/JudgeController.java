package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
@Tag(name = "Judge", description = "APIs for judges to review and manage activity submissions")
public class JudgeController {

    private final JudgeService judgeService;

    @GetMapping("/submissions/pending")
    @Operation(
            summary = "Get all pending submissions",
            description = "Retrieves all activity submissions that have not yet been reviewed by any judge"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved pending submissions"
    )
    public ResponseEntity<List<JudgeSubmissionDTO>> getPendingSubmissions() {
        return ResponseEntity.ok(
                judgeService.getPendingSubmissions()
        );
    }

    @GetMapping("/submissions/activity/{activityId}")
    @Operation(
            summary = "Get submissions for a specific activity",
            description = "Retrieves all submissions related to a given activity ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submissions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<List<JudgeSubmissionDTO>> getSubmissionsForActivity(
            @Parameter(
                    description = "Activity ID",
                    required = true
            )
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(
                judgeService.getSubmissionsForActivity(activityId)
        );
    }

    @PostMapping("/review/{submissionId}")
    @Operation(
            summary = "Review an activity submission",
            description = "Allows a judge to review a submission, award gems, and mark the activity as completed"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Submission already reviewed or invalid request"),
            @ApiResponse(responseCode = "404", description = "Submission or judge not found")
    })
    public ResponseEntity<String> reviewSubmission(
            @Parameter(
                    description = "Submission ID to be reviewed",
                    required = true
            )
            @PathVariable Long submissionId,

            @Parameter(
                    description = "Judge ID who is reviewing the submission",
                    required = true
            )
            @RequestParam Long judgeId
    ) {
        judgeService.reviewSubmission(submissionId, judgeId);
        return ResponseEntity.ok("Submission reviewed successfully");
    }
}
