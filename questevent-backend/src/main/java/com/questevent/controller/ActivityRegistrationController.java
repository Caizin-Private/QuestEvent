package com.questevent.controller;

import com.questevent.dto.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log =
            LoggerFactory.getLogger(ActivityRegistrationController.class);

    private final ActivityRegistrationService activityRegistrationService;

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #request.activityId, authentication.principal.userId)")
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

        log.info("Self activity registration: activityId={}",
                request.getActivityId());

        try {
            ActivityRegistrationResponseDTO response =
                    activityRegistrationService.registerParticipantForActivity(request);

            log.info("Activity registration successful: activityId={}",
                    request.getActivityId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.warn("Activity registration failed: activityId={}, reason={}",
                    request.getActivityId(), e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #activityId, #request.userId)")
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
    public ResponseEntity<ActivityRegistrationResponseDTO> addParticipantToActivity(
            @Parameter(description = "Activity ID", required = true)
            @PathVariable Long activityId,
            @RequestBody AddParticipantInActivityRequestDTO request) {

        log.info("Host adding participant to activity: activityId={}, userId={}",
                activityId, request.getUserId());

        try {
            ActivityRegistrationResponseDTO response =
                    activityRegistrationService.addParticipantToActivity(activityId, request);

            log.info("Participant added to activity: activityId={}, userId={}",
                    activityId, request.getUserId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.warn("Failed to add participant: activityId={}, userId={}, reason={}",
                    activityId, request.getUserId(), e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping
    @Operation(
            summary = "Get all activity registrations",
            description = "Retrieves all activity registrations (Platform Owner only)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {

        log.info("Fetching all activity registrations (platform owner)");

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getAllRegistrations();

        log.debug("Total activity registrations fetched={}", registrations.size());
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(
            summary = "Get registration by ID",
            description = "Retrieves a specific activity registration by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long id) {

        log.info("Fetching activity registration id={}", id);

        try {
            ActivityRegistrationDTO registration =
                    activityRegistrationService.getRegistrationById(id);

            log.debug("Activity registration found id={}", id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            log.warn("Activity registration not found id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, @rbac.getProgramIdByActivityId(#activityId)) "
            + "or @rbac.canJudgeAccessProgram(authentication, @rbac.getProgramIdByActivityId(#activityId))")
    @GetMapping("/activities/{activityId}")
    @Operation(
            summary = "Get registrations by activity",
            description = "Retrieves all registrations for a specific activity"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivity(
            @Parameter(description = "Activity ID", required = true)
            @PathVariable Long activityId) {

        log.info("Fetching registrations for activityId={}", activityId);

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByActivityId(activityId);

        log.debug("Registrations fetched for activityId={}, count={}",
                activityId, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}")
    @Operation(
            summary = "Get registrations by user",
            description = "Retrieves all activity registrations for a specific user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {

        log.info("Fetching activity registrations for userId={}", userId);

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserId(userId);

        log.debug("Registrations fetched for userId={}, count={}",
                userId, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, @rbac.getProgramIdByActivityId(#activityId)) "
            + "or @rbac.canJudgeAccessProgram(authentication, @rbac.getProgramIdByActivityId(#activityId))")
    @GetMapping("/activities/{activityId}/status/{status}")
    @Operation(
            summary = "Get registrations by activity and status",
            description = "Retrieves registrations for an activity filtered by completion status"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivityAndStatus(
            @Parameter(description = "Activity ID", required = true)
            @PathVariable Long activityId,
            @Parameter(description = "Completion status", required = true)
            @PathVariable CompletionStatus status) {

        log.info("Fetching registrations for activityId={} with status={}",
                activityId, status);

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService
                        .getRegistrationsByActivityIdAndStatus(activityId, status);

        log.debug("Registrations fetched for activityId={}, status={}, count={}",
                activityId, status, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("isAuthenticated() and @rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}/status/{status}")
    @Operation(
            summary = "Get registrations by user and status",
            description = "Retrieves activity registrations for a user filtered by completion status"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUserAndStatus(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Completion status", required = true)
            @PathVariable CompletionStatus status) {

        log.info("Fetching registrations for userId={} with status={}",
                userId, status);

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status);

        log.debug("Registrations fetched for userId={}, status={}, count={}",
                userId, status, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update completion status",
            description = "Updates the completion status of an activity registration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ActivityRegistrationDTO> updateCompletionStatus(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long id,
            @RequestBody ActivityCompletionUpdateDTO updateDTO) {

        log.info("Updating completion status for registrationId={}, newStatus={}",
                id, updateDTO.getCompletionStatus());

        try {
            ActivityRegistrationDTO updated =
                    activityRegistrationService.updateCompletionStatus(id, updateDTO);

            log.info("Completion status updated for registrationId={}", id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.warn("Failed to update completion status registrationId={}, reason={}",
                    id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, @rbac.getProgramIdByActivityId(#activityId))")
    @GetMapping("/activities/{activityId}/count")
    @Operation(
            summary = "Get participant count",
            description = "Gets total number of participants registered for an activity"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    public ResponseEntity<Long> getParticipantCount(
            @Parameter(description = "Activity ID", required = true)
            @PathVariable Long activityId) {

        log.info("Fetching participant count for activityId={}", activityId);

        long count =
                activityRegistrationService.getParticipantCountForActivity(activityId);

        log.debug("Participant count for activityId={} is {}", activityId, count);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete registration",
            description = "Deletes an activity registration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long id) {

        log.warn("Deleting activity registration id={}", id);

        try {
            activityRegistrationService.deleteRegistration(id);
            log.info("Activity registration deleted successfully id={}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to delete activity registration id={}, reason={}",
                    id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
