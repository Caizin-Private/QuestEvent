package com.questevent.controller;

import com.questevent.dto.*;
import com.questevent.service.ActivityRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    @Operation(summary = "Register participant for activity")
    @ApiResponse(responseCode = "201", description = "Registration successful")
    public ResponseEntity<ActivityRegistrationResponseDTO> registerParticipant(
            @RequestBody ActivityRegistrationRequestDTO request) {

        log.info("Self activity registration: activityId={}", request.getActivityId());

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.registerParticipantForActivity(request);

        log.info("Activity registration successful: activityId={}", request.getActivityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #activityId, #request.userId)")
    @PostMapping("/activities/{activityId}/participants")
    @Operation(summary = "Add participant to activity (Host only)")
    @ApiResponse(responseCode = "201", description = "Registration successful")
    public ResponseEntity<ActivityRegistrationResponseDTO> addParticipantToActivity(
            @PathVariable UUID activityId,
            @RequestBody AddParticipantInActivityRequestDTO request) {

        log.info("Host adding participant: activityId={}, userId={}",
                activityId, request.getUserId());

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.addParticipantToActivity(activityId, request);

        log.info("Participant added: activityId={}, userId={}",
                activityId, request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {

        log.info("Fetching all activity registrations");
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getAllRegistrations();

        log.debug("Total registrations={}", registrations.size());
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(
            @PathVariable UUID id) {

        log.info("Fetching registration id={}", id);
        ActivityRegistrationDTO registration =
                activityRegistrationService.getRegistrationById(id);

        return ResponseEntity.ok(registration);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, @rbac.getProgramIdByActivityId(#activityId))")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable UUID id) {

        log.warn("Deleting registration id={}", id);
        activityRegistrationService.deleteRegistration(id);

        return ResponseEntity.noContent().build();
    }
}
