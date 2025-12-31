package com.questevent.controller;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.dto.ActivityResponseDTO;
import com.questevent.entity.Activity;
import com.questevent.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/programs/{programId}/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponseDTO> createActivity(
            @PathVariable Long programId, @RequestBody ActivityRequestDTO dto) {
        Activity activity = activityService.createActivity(programId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDTO(activity));
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponseDTO>> getActivities(@PathVariable Long programId) {
        return ResponseEntity.ok(activityService.getActivitiesByProgramId(programId)
                .stream().map(this::convertToResponseDTO).toList());
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponseDTO> updateActivity(
            @PathVariable Long programId, 
            @PathVariable Long activityId, 
            @RequestBody ActivityRequestDTO dto) {
        Activity updated = activityService.updateActivity(programId, activityId, dto);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long programId, @PathVariable Long activityId) {
        activityService.deleteActivity(programId, activityId);
        return ResponseEntity.noContent().build();
    }

    private ActivityResponseDTO convertToResponseDTO(Activity activity) {
        ActivityResponseDTO response = new ActivityResponseDTO();
        response.setActivityId(activity.getActivityId());
        response.setProgramId(activity.getProgram().getProgramId());
        response.setActivityName(activity.getActivityName());
        response.setActivityDuration(activity.getActivityDuration());
        response.setActivityRulebook(activity.getActivityRulebook());
        response.setActivityDescription(activity.getActivityDescription());
        response.setRewardGems(activity.getRewardGems());
        response.setCreatedAt(activity.getCreatedAt());
        response.setIsCompulsory(activity.getIsCompulsory());
        return response;
    }
}