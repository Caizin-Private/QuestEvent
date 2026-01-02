package com.questevent.controller;

import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-registrations")
@RequiredArgsConstructor
public class ActivityRegistrationController {

    private final ActivityRegistrationService activityRegistrationService;

    @PostMapping
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
    public ResponseEntity<List<ActivityRegistrationDTO>> getAllRegistrations() {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getAllRegistrations();
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityRegistrationDTO> getRegistrationById(@PathVariable Long id) {
        try {
            ActivityRegistrationDTO registration =
                    activityRegistrationService.getRegistrationById(id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivity(
            @PathVariable Long activityId) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByActivityId(activityId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUser(
            @PathVariable Long userId) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserId(userId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/activities/{activityId}/status/{status}")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByActivityAndStatus(
            @PathVariable Long activityId,
            @PathVariable CompletionStatus status) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByActivityIdAndStatus(activityId, status);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/users/{userId}/status/{status}")
    public ResponseEntity<List<ActivityRegistrationDTO>> getRegistrationsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable CompletionStatus status) {
        List<ActivityRegistrationDTO> registrations =
                activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(registrations);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ActivityRegistrationDTO> updateCompletionStatus(
            @PathVariable Long id,
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
    public ResponseEntity<Long> getParticipantCount(@PathVariable Long activityId) {
        long count = activityRegistrationService.getParticipantCountForActivity(activityId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {
        try {
            activityRegistrationService.deleteRegistration(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}