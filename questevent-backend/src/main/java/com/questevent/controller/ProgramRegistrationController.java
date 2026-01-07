package com.questevent.controller;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.service.ProgramRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    private final ProgramRegistrationService programRegistrationService;

    /* ===================== SELF REGISTRATION ===================== */

    @PreAuthorize("@rbac.canRegisterForProgram(authentication, #request.programId, #request.userId)")
    @PostMapping
    @Operation(
            summary = "Register participant for program",
            description = "Registers a user for a specific program (self-registration)"
    )
    @ApiResponse(responseCode = "201", description = "Registration successful")
    @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    public ResponseEntity<ProgramRegistrationResponseDTO> registerParticipant(
            @RequestBody ProgramRegistrationRequestDTO request
    ) {
        ProgramRegistrationResponseDTO response =
                programRegistrationService.registerParticipantForProgram(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ===================== HOST REGISTRATION ===================== */

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
            @PathVariable Long programId,

            @Parameter(description = "User ID to register", required = true)
            @RequestParam Long userId
    ) {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(programId);
        request.setUserId(userId);

        ProgramRegistrationResponseDTO response =
                programRegistrationService.registerParticipantForProgram(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ===================== READ ===================== */

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all program registrations")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getAllRegistrations() {
        return ResponseEntity.ok(
                programRegistrationService.getAllRegistrations()
        );
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(summary = "Get registration by ID")
    @ApiResponse(responseCode = "200", description = "Registration found")
    @ApiResponse(responseCode = "404", description = "Registration not found")
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                programRegistrationService.getRegistrationById(id)
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}")
    @Operation(summary = "Get registrations by program")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByProgram(
            @PathVariable Long programId
    ) {
        return ResponseEntity.ok(
                programRegistrationService.getRegistrationsByProgramId(programId)
        );
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get registrations by user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                programRegistrationService.getRegistrationsByUserId(userId)
        );
    }

    /* ===================== DELETE ===================== */

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete registration")
    @ApiResponse(responseCode = "204", description = "Registration deleted successfully")
    public ResponseEntity<Void> deleteRegistration(
            @PathVariable Long id
    ) {
        programRegistrationService.deleteRegistration(id);
        return ResponseEntity.noContent().build();
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
            @PathVariable Long programId,
            @PathVariable Long userId
    ) {
        programRegistrationService.deleteRegistrationByProgramAndUser(programId, userId);
        return ResponseEntity.noContent().build();
    }
}
