package com.questevent.controller;

import com.questevent.entity.ActivitySubmission;
import com.questevent.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    /**
     * Fetch all submissions for a given activity
     * Used by Judge Dashboard
     */
    @GetMapping("/submissions/{activityId}")
    public ResponseEntity<List<ActivitySubmission>> getSubmissionsForActivity(
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(
                judgeService.getSubmissionsForActivity(activityId)
        );
    }

    /**
     * Review a submission and award gems
     */
    @PostMapping("/review/{submissionId}")
    public ResponseEntity<String> reviewSubmission(
            @PathVariable Long submissionId,
            @RequestParam Integer awardedGems
    ) {
        judgeService.reviewSubmission(submissionId, awardedGems);
        return ResponseEntity.ok("Submission reviewed successfully");
    }
}
