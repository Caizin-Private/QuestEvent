package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionDto;
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
    public ResponseEntity<List<ActivitySubmissionDto>> getSubmissionsForActivity(
            @PathVariable Long activityId
    ) {
        List<ActivitySubmissionDto> response =
                judgeService.getSubmissionsForActivity(activityId)
                        .stream()
                        .map(submission -> new ActivitySubmissionDto(
                                submission.getSubmissionId(),
                                submission.getActivityRegistration().getActivity().getActivityId(),
                                submission.getActivityRegistration().getUser().getUserId(),
                                submission.getSubmissionUrl(),
                                submission.getAwardedGems(),
                                submission.getSubmittedAt(),
                                submission.getReviewedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Review a submission and award gems
     */

    @PostMapping("/review/{submissionId}")
    public ResponseEntity<String> reviewSubmission(
            @PathVariable Long submissionId,
            @RequestParam Integer awardedGems
    ){
        judgeService.reviewSubmission(submissionId, awardedGems);
        return ResponseEntity.ok("Submission reviewed successfully");
    }
}
