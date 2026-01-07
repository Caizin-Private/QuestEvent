package com.questevent.controller;

import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/activity-registrations")
@RequiredArgsConstructor
@Tag(name = "Activity Registrations", description = "Activity registration management APIs")
public class ActivityRegistrationController {

    private final ActivityRegistrationService activityRegistrationService;

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #request.activityId, #request.userId)")
    @PostMapping
    @Operation(
            summary = "Register participant for activity",
            description = "Registers a user for a specific activity (self-registration)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    })
    public ResponseEntity<ActivityRegistrationResponseDTO> registerParticipant(
            @RequestBody ActivityRegistrationRequestDTO request) {

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.registerParticipantForActivity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #activityId)")
    @PostMapping("/activities/{activityId}/participants")
    @Operation(
            summary = "Add participant to activity (Host only)",
            description = "Allows program host to register a user for an activity in their program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Activity or user not found")
    })
    public ResponseEntity<ActivityRegistrationResponseDTO> addParticipantByHost(
            @PathVariable Long activityId,
            @RequestParam Long userId,
            @RequestParam(required = false) CompletionStatus completionStatus) {

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);
        request.setUserId(userId);
        request.setCompletionStatus(
                completionStatus != null ? completionStatus : CompletionStatus.NOT_COMPLETED
        );

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.registerParticipantForActivity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all activity registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {
        return ResponseEntity.ok(activityRegistrationService.getAllRegistrations());
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(summary = "Get registration by ID")
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(
            @PathVariable Long id) {

        ActivityRegistrationDTO registration =
                activityRegistrationService.getRegistrationById(id);

        return ResponseEntity.ok(registration);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #activityId)")
    @GetMapping("/activities/{activityId}")
    @Operation(summary = "Get registrations by activity")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivity(
            @PathVariable Long activityId) {

        return ResponseEntity.ok(
                activityRegistrationService.getRegistrationsByActivityId(activityId)
        );
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get registrations by user")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                activityRegistrationService.getRegistrationsByUserId(userId)
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #activityId)")
    @GetMapping("/activities/{activityId}/status/{status}")
    @Operation(summary = "Get registrations by activity and status")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivityAndStatus(
            @PathVariable Long activityId,
            @PathVariable CompletionStatus status) {

        return ResponseEntity.ok(
                activityRegistrationService.getRegistrationsByActivityIdAndStatus(activityId, status)
        );
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}/status/{status}")
    @Operation(summary = "Get registrations by user and status")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable CompletionStatus status) {

        return ResponseEntity.ok(
                activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status)
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #id)")
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update completion status")
    public ResponseEntity<ActivityRegistrationDTO> updateCompletionStatus(
            @PathVariable Long id,
            @RequestBody ActivityCompletionUpdateDTO updateDTO) {

        ActivityRegistrationDTO updated =
                activityRegistrationService.updateCompletionStatus(id, updateDTO);

        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #activityId)")
    @GetMapping("/activities/{activityId}/count")
    @Operation(summary = "Get participant count")
    public ResponseEntity<Long> getParticipantCount(
            @PathVariable Long activityId) {

        return ResponseEntity.ok(
                activityRegistrationService.getParticipantCountForActivity(activityId)
        );
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete registration")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {

        activityRegistrationService.deleteRegistration(id);
        return ResponseEntity.noContent().build();
    }
}
