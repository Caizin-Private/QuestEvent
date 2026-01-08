package com.questevent.controller;

import com.questevent.dto.AddParticipantInProgramRequestDTO;
import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.service.ProgramRegistrationService;
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
@RequestMapping("/api/program-registrations")
@RequiredArgsConstructor
@Tag(name = "Program Registrations", description = "Program registration management APIs")
public class ProgramRegistrationController {

    private static final Logger log =
            LoggerFactory.getLogger(ProgramRegistrationController.class);

    private final ProgramRegistrationService programRegistrationService;

    @PreAuthorize("@rbac.canRegisterForProgram(authentication, #request.programId, authentication.principal.userId)")
    @PostMapping
    public ResponseEntity<ProgramRegistrationResponseDTO> registerParticipant(
            @RequestBody ProgramRegistrationRequestDTO request) {

        log.info("Self registration request: programId={}",
                request.getProgramId());

        try {
            ProgramRegistrationResponseDTO response =
                    programRegistrationService.registerParticipantForProgram(request);

            log.info("Self registration successful: programId={}",
                    request.getProgramId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.warn("Self registration failed: programId={}, reason={}",
                    request.getProgramId(), e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping("/programs/{programId}/participants")
    public ResponseEntity<ProgramRegistrationResponseDTO> addParticipantByHost(
            @PathVariable Long programId,
            @RequestBody AddParticipantInProgramRequestDTO request) {

        log.info("Host adding participant: programId={}, userId={}",
                programId, request.getUserId());

        ProgramRegistrationResponseDTO response =
                programRegistrationService.addParticipantToProgram(programId, request);

        log.info("Participant added by host: programId={}, userId={}",
                programId, request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping
    public ResponseEntity<List<ProgramRegistrationDTO>> getAllRegistrations() {
        log.info("Fetching all program registrations (platform owner)");

        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getAllRegistrations();

        log.debug("Total registrations fetched={}", registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(@PathVariable Long id) {
        log.info("Fetching program registration id={}", id);

        try {
            ProgramRegistrationDTO registration =
                    programRegistrationService.getRegistrationById(id);

            log.debug("Registration found id={}", id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            log.warn("Registration not found id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId) or @rbac.canJudgeAccessProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByProgram(
            @PathVariable Long programId) {

        log.info("Fetching registrations for programId={}", programId);

        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getRegistrationsByProgramId(programId);

        log.debug("Registrations fetched for programId={}, count={}",
                programId, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByUser(
            @PathVariable Long userId) {

        log.info("Fetching registrations for userId={}", userId);

        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getRegistrationsByUserId(userId);

        log.debug("Registrations fetched for userId={}, count={}",
                userId, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId) or @rbac.canJudgeAccessProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}/count")
    public ResponseEntity<Long> getParticipantCount(@PathVariable Long programId) {
        log.info("Fetching participant count for programId={}", programId);

        long count =
                programRegistrationService.getParticipantCountForProgram(programId);

        log.debug("Participant count for programId={} is {}", programId, count);

        return ResponseEntity.ok(count);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {
        log.warn("Deleting program registration id={}", id);

        try {
            programRegistrationService.deleteRegistration(id);
            log.info("Registration deleted successfully id={}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to delete registration id={}, reason={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/programs/{programId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipantByHost(
            @PathVariable Long programId,
            @PathVariable Long userId) {

        log.warn("Host removing participant: programId={}, userId={}",
                programId, userId);

        try {
            programRegistrationService.deleteRegistrationByProgramAndUser(programId, userId);
            log.info("Participant removed: programId={}, userId={}",
                    programId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to remove participant: programId={}, userId={}, reason={}",
                    programId, userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
