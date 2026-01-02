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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-registrations")
@RequiredArgsConstructor
@Tag(name = "Activity Registrations", description = "Activity registration management APIs")
public class ActivityRegistrationController {

    private final ActivityRegistrationService activityRegistrationService;

    @PostMapping
    @Operation(summary = "Register participant for activity", description = "Registers a user for a specific activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    })
    public ResponseEntity<ActivityRegistrationResponseDTO> registerParticipant(
            @RequestBody ActivityRegistrationRequestDTO request) {
        try {
            ActivityRegistrationResponseDTO response =
                    activityRegistrationService.registerParticipantForActivity(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Get all activity registrations", description = "Retrieves all activity registrations")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getAllRegistrations();
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get registration by ID", description = "Retrieves a specific activity registration by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(
            @Parameter(description = "Registration ID", required = true) @PathVariable Long id) {
        try {
            ActivityRegistrationDTO registration =
                    activityRegistrationService.getRegistrationById(id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/activities/{activityId}")
    @Operation(summary = "Get registrations by activity", description = "Retrieves all registrations for a specific activity")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivity(
            @Parameter(description = "Activity ID", required = true) @PathVariable Long activityId) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByActivityId(activityId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get registrations by user", description = "Retrieves all activity registrations for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserId(userId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/activities/{activityId}/status/{status}")
    @Operation(summary = "Get registrations by activity and status", description = "Retrieves registrations for an activity filtered by completion status")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivityAndStatus(
            @Parameter(description = "Activity ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "Completion status", required = true) @PathVariable CompletionStatus status) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByActivityIdAndStatus(activityId, status);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/users/{userId}/status/{status}")
    @Operation(summary = "Get registrations by user and status", description = "Retrieves activity registrations for a user filtered by completion status")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUserAndStatus(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Completion status", required = true) @PathVariable CompletionStatus status) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(registrations);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update completion status", description = "Updates the completion status of an activity registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ActivityRegistrationDTO> updateCompletionStatus(
            @Parameter(description = "Registration ID", required = true) @PathVariable Long id,
            @RequestBody ActivityCompletionUpdateDTO updateDTO) {
        try {
            ActivityRegistrationDTO updatedRegistration =
                    activityRegistrationService.updateCompletionStatus(id, updateDTO);
            return ResponseEntity.ok(updatedRegistration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/activities/{activityId}/count")
    @Operation(summary = "Get participant count", description = "Gets the total number of participants registered for an activity")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    public ResponseEntity<Long> getParticipantCount(
            @Parameter(description = "Activity ID", required = true) @PathVariable Long activityId) {
        long count = activityRegistrationService.getParticipantCountForActivity(activityId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete registration", description = "Deletes an activity registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true) @PathVariable Long id) {
        try {
            activityRegistrationService.deleteRegistration(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}