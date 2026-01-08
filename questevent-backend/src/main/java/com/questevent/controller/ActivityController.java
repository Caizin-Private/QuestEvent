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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs/{programId}/activities")
@Tag(name = "Activities", description = "Activity management APIs")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping
    @Operation(summary = "Create a new activity", description = "Creates a new activity for a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Activity created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<ActivityResponseDTO> createActivity(
            @Parameter(description = "Program ID", required = true) @PathVariable UUID programId,
            @RequestBody ActivityRequestDTO dto) {
        Activity activity = activityService.createActivity(programId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDTO(activity));
    }

    @PreAuthorize("@rbac.canViewProgram(authentication, #programId)")
    @GetMapping
    @Operation(summary = "Get all activities for a program", description = "Retrieves all activities associated with a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved activities"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<List<ActivityResponseDTO>> getActivities(
            @Parameter(description = "Program ID", required = true) @PathVariable UUID programId) {
        return ResponseEntity.ok(activityService.getActivitiesByProgramId(programId)
                .stream().map(this::convertToResponseDTO).toList());
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PutMapping("/{activityId}")
    @Operation(summary = "Update activity", description = "Updates an existing activity's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity updated successfully"),
            @ApiResponse(responseCode = "404", description = "Activity or program not found")
    })
    public ResponseEntity<ActivityResponseDTO> updateActivity(
            @Parameter(description = "Program ID", required = true) @PathVariable UUID programId,
            @Parameter(description = "Activity ID", required = true) @PathVariable UUID activityId,
            @RequestBody ActivityRequestDTO dto) {
        Activity updated = activityService.updateActivity(programId, activityId, dto);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{activityId}")
    @Operation(summary = "Delete activity", description = "Deletes an activity from a program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Activity deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Activity or program not found")
    })
    public ResponseEntity<Void> deleteActivity(
            @Parameter(description = "Program ID", required = true) @PathVariable UUID programId,
            @Parameter(description = "Activity ID", required = true) @PathVariable UUID activityId) {
        activityService.deleteActivity(programId, activityId);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("@rbac.canViewProgram(authentication, #programId)")
    @GetMapping("/compulsory")
    @Operation(summary = "Get compulsory activities for a program", description = "Retrieves all compulsory activities (isCompulsory=true) associated with a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved activities"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<List<ActivityResponseDTO>> getCompulsoryActivities(
            @Parameter(description = "Program ID", required = true) @PathVariable UUID programId) {
        return ResponseEntity.ok(activityService.getCompulsoryActivitiesByProgramId(programId)
                .stream().map(this::convertToResponseDTO).toList());
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