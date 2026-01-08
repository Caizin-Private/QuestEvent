package com.questevent.controller;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.dto.ActivityResponseDTO;
import com.questevent.entity.Activity;
import com.questevent.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs/{programId}/activities")
@Tag(name = "Activities", description = "Activity management APIs")
public class ActivityController {

    private static final Logger log =
            LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping
    public ResponseEntity<ActivityResponseDTO> createActivity(
            @PathVariable Long programId,
            @RequestBody ActivityRequestDTO dto) {

        log.info("Creating activity for programId={}, activityName={}",
                programId, dto.getActivityName());

        Activity activity = activityService.createActivity(programId, dto);

        log.info("Activity created successfully activityId={}, programId={}",
                activity.getActivityId(), programId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertToResponseDTO(activity));
    }

    @PreAuthorize("@rbac.canViewProgram(authentication, #programId)")
    @GetMapping
    public ResponseEntity<List<ActivityResponseDTO>> getActivities(
            @PathVariable Long programId) {

        log.info("Fetching activities for programId={}", programId);

        List<ActivityResponseDTO> response =
                activityService.getActivitiesByProgramId(programId)
                        .stream()
                        .map(this::convertToResponseDTO)
                        .toList();

        log.debug("Activities fetched for programId={}, count={}",
                programId, response.size());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponseDTO> updateActivity(
            @PathVariable Long programId,
            @PathVariable Long activityId,
            @RequestBody ActivityRequestDTO dto) {

        log.info("Updating activity activityId={}, programId={}",
                activityId, programId);

        Activity updated =
                activityService.updateActivity(programId, activityId, dto);

        log.info("Activity updated successfully activityId={}, programId={}",
                activityId, programId);

        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long programId,
            @PathVariable Long activityId) {

        log.warn("Deleting activity activityId={}, programId={}",
                activityId, programId);

        activityService.deleteActivity(programId, activityId);

        log.info("Activity deleted successfully activityId={}, programId={}",
                activityId, programId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@rbac.canViewProgram(authentication, #programId)")
    @GetMapping("/compulsory")
    public ResponseEntity<List<ActivityResponseDTO>> getCompulsoryActivities(
            @PathVariable Long programId) {

        log.info("Fetching compulsory activities for programId={}", programId);

        List<ActivityResponseDTO> response =
                activityService.getCompulsoryActivitiesByProgramId(programId)
                        .stream()
                        .map(this::convertToResponseDTO)
                        .toList();

        log.debug("Compulsory activities fetched for programId={}, count={}",
                programId, response.size());

        return ResponseEntity.ok(response);
    }

    private ActivityResponseDTO convertToResponseDTO(Activity activity) {
        log.debug("Converting Activity entity to DTO activityId={}",
                activity.getActivityId());

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
