package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.JudgeSubmissionDetailsDTO;
import com.questevent.dto.JudgeSubmissionStatsDTO;
import com.questevent.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
@Tag(
        name = "Judge APIs",
        description = "APIs for judges to view and review activity submissions " +
                "only for programs they are assigned to"
)
public class JudgeController {

    private static final Logger log =
            LoggerFactory.getLogger(JudgeController.class);

    private final JudgeService judgeService;

    @Operation(
            summary = "Get pending submissions for judge",
            description = "Returns all PENDING submissions for activities " +
                    "belonging to programs where the logged-in judge is assigned"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pending submissions fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JudgeSubmissionDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized – JWT missing or invalid"
            )
    })
    @PreAuthorize(
            "@rbac.isPlatformOwner(authentication) " +
                    "or @rbac.canJudgeAccessAnyProgram(authentication)"
    )
    @GetMapping("/submissions/pending")
    public ResponseEntity<List<JudgeSubmissionDTO>> getPendingSubmissions() {
        log.info("Fetching pending submissions for judge");

        List<JudgeSubmissionDTO> submissions =
                judgeService.getPendingSubmissionsForJudge(null);

        log.debug("Pending submissions fetched, count={}", submissions.size());
        return ResponseEntity.ok(submissions);
    }

    @Operation(
            summary = "Get pending submissions for an activity",
            description = "Returns all PENDING submissions for a specific activity. " +
                    "Judge must be assigned to the program of that activity."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pending submissions for activity fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized – Judge not assigned or JWT invalid"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Activity not found"
            )
    })
    @PreAuthorize(
            "@rbac.isPlatformOwner(authentication) " +
                    "or @rbac.canJudgeAccessProgram(authentication, " +
                    "@rbac.getProgramIdByActivityId(#activityId))"
    )
    @GetMapping("/submissions/pending/activity/{activityId}")
    public ResponseEntity<List<JudgeSubmissionDTO>> getPendingSubmissionsForActivity(
            @Parameter(
                    name = "activityId",
                    description = "ID of the activity",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable UUID activityId) {
        log.info("Fetching pending submissions for activityId={}", activityId);

        List<JudgeSubmissionDTO> submissions =
                judgeService.getPendingSubmissionsForActivity(activityId, null);

        log.debug("Pending submissions fetched for activityId={}, count={}",
                activityId, submissions.size());

        return ResponseEntity.ok(submissions);
    }

    @Operation(
            summary = "Get all submissions for judge",
            description = "Returns all submissions (PENDING + REVIEWED) " +
                    "for programs where the judge is assigned. " +
                    "OWNER role sees all submissions."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All submissions fetched successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PreAuthorize(
            "@rbac.isPlatformOwner(authentication) " +
                    "or @rbac.canJudgeAccessAnyProgram(authentication)"
    )
    @GetMapping("/submissions")
    public ResponseEntity<List<JudgeSubmissionDTO>> getAllSubmissions() {
        log.info("Fetching all submissions for judge");

        List<JudgeSubmissionDTO> submissions =
                judgeService.getAllSubmissionsForJudge(null);

        log.debug("All submissions fetched, count={}", submissions.size());
        return ResponseEntity.ok(submissions);
    }

    @Operation(
            summary = "Review a submission",
            description = "Approves a submission, awards reward gems, " +
                    "and marks activity as completed. " +
                    "Only the assigned judge can review."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Submission reviewed successfully"

            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid submission or already reviewed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized – Judge not assigned"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Submission not found"
            )
    })
    @PreAuthorize(
            "@rbac.canJudgeAccessSubmission(authentication, #submissionId)"
    )
    @PatchMapping("/submissions/{submissionId}/review")
    public ResponseEntity<Void> reviewSubmission(
            @Parameter(
                    name = "submissionId",
                    description = "ID of the submission to review",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable UUID submissionId
    ) {
        log.info("Reviewing submission submissionId={}", submissionId);

        judgeService.reviewSubmission(submissionId);

        log.info("Submission reviewed successfully submissionId={}", submissionId);
        return ResponseEntity.ok().build();
    }


    @Operation(
            summary = "Reject a submission",
            description = "Rejects a submission with a reason. " +
                    "Only the assigned judge can reject a pending submission."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Submission rejected successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Submission already reviewed or invalid"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized – Judge not assigned"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Submission not found"
            )
    })
    @PreAuthorize(
            "@rbac.canJudgeAccessSubmission(authentication, #submissionId)"
    )
    @PatchMapping("/submissions/{submissionId}/reject")
    public ResponseEntity<Void> rejectSubmission(
            @PathVariable UUID submissionId) {
        log.info("Rejecting submission submissionId={}", submissionId);

        judgeService.rejectSubmission(submissionId,null);

        log.info("Submission rejected successfully submissionId={}", submissionId);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize(
            "@rbac.canJudgeAccessSubmission(authentication, #submissionId)"
    )
    @GetMapping("/submissions/{submissionId}")
    @Operation(
            summary = "Get submission details for judge",
            description = "Fetch full submission details for review. " +
                    "Only the assigned judge can access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission details fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Submission not found")
    })
    public ResponseEntity<JudgeSubmissionDetailsDTO> getSubmissionDetails(
            @PathVariable UUID submissionId) {
        log.info("Fetching submission details for judge | submissionId={}", submissionId);

        return ResponseEntity.ok(
                judgeService.getSubmissionDetails(submissionId, null)
        );
    }


    @PreAuthorize(
            "@rbac.isPlatformOwner(authentication) " +
                    "or @rbac.canJudgeAccessAnyProgram(authentication)"
    )
    @GetMapping("/submissions/stats")
    @Operation(
            summary = "Get submission statistics for judge",
            description = "Returns counts of pending, approved, and rejected submissions " +
                    "for programs judged by the logged-in judge"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission stats fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<JudgeSubmissionStatsDTO> getSubmissionStats() {
        log.info("Fetching submission stats for judge");

        return ResponseEntity.ok(
                judgeService.getSubmissionStats(null)
        );
    }
}
