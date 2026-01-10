package com.questevent.controller;

import com.questevent.dto.*;
import com.questevent.service.ProgramRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @ApiResponse(responseCode = "201", description = "Registration successful")
    @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    public ResponseEntity<ProgramRegistrationResponseDTO> registerParticipant(
            @RequestBody ProgramRegistrationRequestDTO request) {

        log.info("Self registration request: programId={}", request.getProgramId());

        ProgramRegistrationResponseDTO response =
                programRegistrationService.registerParticipantForProgram(request);

        log.info("Self registration successful: programId={}", request.getProgramId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping("/programs/{programId}/participants")
    @Operation(
            summary = "Add participant to program (Host only)",
            description = "Allows program host to register a user for their program"
    )
    @ApiResponse(responseCode = "201", description = "Registration successful")
    @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    @ApiResponse(responseCode = "403", description = "Permission denied")
    @ApiResponse(responseCode = "404", description = "Program or user not found")
    public ResponseEntity<ProgramRegistrationResponseDTO> addParticipantByHost(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId,
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
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    @ApiResponse(responseCode = "403", description = "Forbidden")
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
    @ApiResponse(responseCode = "200", description = "Registration found")
    @ApiResponse(responseCode = "404", description = "Registration not found")
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable UUID id) {

        log.info("Fetching program registration id={}", id);

        try {
            return ResponseEntity.ok(
                    programRegistrationService.getRegistrationById(id)
            );
        } catch (RuntimeException e) {
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
            @PathVariable UUID programId) {

        return ResponseEntity.ok(
                programRegistrationService.getRegistrationsByProgramId(programId)
        );
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

        return ResponseEntity.ok(
                programRegistrationService.getRegistrationsByUserId(userId)
        );
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
            @PathVariable UUID programId) {

        return ResponseEntity.ok(
                programRegistrationService.getParticipantCountForProgram(programId)
        );
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete registration",
            description = "Deletes a program registration"
    )
    @ApiResponse(responseCode = "204", description = "Registration deleted successfully")
    @ApiResponse(responseCode = "404", description = "Registration not found")
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable UUID id) {

        try {
            programRegistrationService.deleteRegistration(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/programs/{programId}/participants/{userId}")
    @Operation(
            summary = "Remove participant from program (Host only)",
            description = "Allows program host to remove a user from their program"
    )
    @ApiResponse(responseCode = "204", description = "Participant removed successfully")
    @ApiResponse(responseCode = "403", description = "Permission denied")
    @ApiResponse(responseCode = "404", description = "Registration not found")
    public ResponseEntity<Void> removeParticipantByHost(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId,
            @Parameter(description = "User ID to remove", required = true)
            @PathVariable Long userId) {

        try {
            programRegistrationService
                    .deleteRegistrationByProgramAndUser(programId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
