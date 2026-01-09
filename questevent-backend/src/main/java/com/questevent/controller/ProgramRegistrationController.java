package com.questevent.controller;

import com.questevent.dto.*;
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
    @Operation(
            summary = "Register participant for program",
            description = "Registers a user for a specific program (self-registration)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    })
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
    @Operation(
            summary = "Add participant to program (Host only)",
            description = "Allows program host to register a user for their program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program or user not found")
    })
    public ResponseEntity<ProgramRegistrationResponseDTO> addParticipantByHost(
            @Parameter(description = "Program ID", required = true)
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
    @Operation(
            summary = "Get all program registrations",
            description = "Retrieves all program registrations (Platform Owner only)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ProgramRegistrationDTO>> getAllRegistrations() {

        log.info("Fetching all program registrations (platform owner)");

        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getAllRegistrations();

        log.debug("Total registrations fetched={}", registrations.size());
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(
            summary = "Get registration by ID",
            description = "Retrieves a specific program registration by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long id) {

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

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId) "
            + "or @rbac.canJudgeAccessProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}")
    @Operation(
            summary = "Get registrations by program",
            description = "Retrieves all registrations for a specific program"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByProgram(
            @Parameter(description = "Program ID", required = true)
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
    @Operation(
            summary = "Get registrations by user",
            description = "Retrieves all program registrations for a specific user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {

        log.info("Fetching registrations for userId={}", userId);

        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getRegistrationsByUserId(userId);

        log.debug("Registrations fetched for userId={}, count={}",
                userId, registrations.size());

        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId) "
            + "or @rbac.canJudgeAccessProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}/count")
    @Operation(
            summary = "Get participant count",
            description = "Gets the total number of participants registered for a program"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    public ResponseEntity<Long> getParticipantCount(
            @Parameter(description = "Program ID", required = true)
            @PathVariable Long programId) {

        log.info("Fetching participant count for programId={}", programId);

        long count =
                programRegistrationService.getParticipantCountForProgram(programId);

        log.debug("Participant count for programId={} is {}", programId, count);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete registration",
            description = "Deletes a program registration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long id) {

        log.warn("Deleting program registration id={}", id);

        try {
            programRegistrationService.deleteRegistration(id);
            log.info("Registration deleted successfully id={}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to delete registration id={}, reason={}",
                    id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/programs/{programId}/participants/{userId}")
    @Operation(
            summary = "Remove participant from program (Host only)",
            description = "Allows program host to remove a user from their program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Participant removed successfully"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<Void> removeParticipantByHost(
            @Parameter(description = "Program ID", required = true)
            @PathVariable Long programId,
            @Parameter(description = "User ID to remove", required = true)
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
