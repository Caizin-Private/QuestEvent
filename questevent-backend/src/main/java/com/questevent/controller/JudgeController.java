package com.questevent.controller;

import com.questevent.dto.ActivitySubmissionDto;
import com.questevent.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;



    @PreAuthorize("hasAnyRole('OWNER', 'JUDGE')")
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

    @PreAuthorize("@rbac.canVerifySubmission(authentication, #submissionId)")
    @PostMapping("/review/{submissionId}")
    public ResponseEntity<String> reviewSubmission(
            @PathVariable Long submissionId
    ) {
        judgeService.reviewSubmission(submissionId);
        return ResponseEntity.ok("Submission reviewed successfully");
    }

}
