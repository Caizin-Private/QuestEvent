package com.questevent.controller;

import com.questevent.dto.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
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
    public ResponseEntity<ActivityRegistrationResponseDTO> registerParticipant(
            @RequestBody ActivityRegistrationRequestDTO request) {

        log.info("Self activity registration: activityId={}",
                request.getActivityId());


        ActivityRegistrationResponseDTO response =
                activityRegistrationService.registerParticipantForActivity(request);

        log.info("Activity registration successful: activityId={}", request.getActivityId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #activityId, #request.userId)")
    @PostMapping("/activities/{activityId}/participants")
    public ResponseEntity<ActivityRegistrationResponseDTO> addParticipantToActivity(
            @PathVariable Long activityId,
            @RequestBody AddParticipantInActivityRequestDTO request) {

        log.info("Host adding participant to activity: activityId={}, userId={}",
                activityId, request.getUserId());

        ActivityRegistrationResponseDTO response =
                activityRegistrationService.addParticipantToActivity(activityId, request);

        log.info("Participant added to activity: activityId={}, userId={}",
                activityId, request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {
        log.info("Fetching all activity registrations (platform owner)");

        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getAllRegistrations();

        log.debug("Total activity registrations fetched={}", registrations.size());
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(@PathVariable Long id) {
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
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivity(
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
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUser(
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
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivityAndStatus(
            @PathVariable Long activityId,
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
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUserAndStatus(
            @PathVariable Long userId,
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
    public ResponseEntity<ActivityRegistrationDTO> updateCompletionStatus(
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
    public ResponseEntity<Long> getParticipantCount(@PathVariable Long activityId) {

        log.info("Fetching participant count for activityId={}", activityId);

        long count =
                activityRegistrationService.getParticipantCountForActivity(activityId);

        log.debug("Participant count for activityId={} is {}", activityId, count);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("@rbac.canAccessActivityRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {

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
