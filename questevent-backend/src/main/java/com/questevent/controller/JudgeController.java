package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionDto;
import com.questevent.dto.JudgeSubmissionDto;
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



    @GetMapping("/submissions/{activityId}")
    public ResponseEntity<List<JudgeSubmissionDto>> getSubmissionsForActivity(
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(
                judgeService.getSubmissionsForActivity(activityId)
        );
    }


    @PostMapping("/review/{submissionId}")
    public ResponseEntity<String> reviewSubmission(
            @PathVariable Long submissionId,
            @RequestParam Long judgeId
    ) {
        judgeService.reviewSubmission(submissionId, judgeId);
        return ResponseEntity.ok("Submission reviewed successfully");
    }

}
