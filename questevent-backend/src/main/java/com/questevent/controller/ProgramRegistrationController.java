package com.questevent.controller;

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

    @PreAuthorize("@rbac.canRegisterForProgram(authentication, #request.programId, #request.userId)")
    @PostMapping
    @Operation(summary = "Register participant for program", description = "Registers a user for a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already registered")
    })
    public ResponseEntity<ProgramRegistrationResponseDTO> registerParticipant(
            @RequestBody ProgramRegistrationRequestDTO request) {
        try {
            ProgramRegistrationResponseDTO response =
                    programRegistrationService.registerParticipantForProgram(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('OWNER', 'JUDGE')")
    @GetMapping
    @Operation(summary = "Get all program registrations", description = "Retrieves all program registrations")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getAllRegistrations() {
        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getAllRegistrations();
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(summary = "Get registration by ID", description = "Retrieves a specific program registration by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(
            @Parameter(description = "Registration ID", required = true) @PathVariable Long id) {
        try {
            ProgramRegistrationDTO registration =
                    programRegistrationService.getRegistrationById(id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}")
    @Operation(summary = "Get registrations by program", description = "Retrieves all registrations for a specific program")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByProgram(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getRegistrationsByProgramId(programId);
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #userId)")
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get registrations by user", description = "Retrieves all program registrations for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved registrations")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        List<ProgramRegistrationDTO> registrations =
                programRegistrationService.getRegistrationsByUserId(userId);
        return ResponseEntity.ok(registrations);
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @GetMapping("/programs/{programId}/count")
    @Operation(summary = "Get participant count", description = "Gets the total number of participants registered for a program")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved count")
    public ResponseEntity<Long> getParticipantCount(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        long count = programRegistrationService.getParticipantCountForProgram(programId);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("@rbac.canAccessProgramRegistration(authentication, #id)")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete registration", description = "Deletes a program registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<Void> deleteRegistration(
            @Parameter(description = "Registration ID", required = true) @PathVariable Long id) {
        try {
            programRegistrationService.deleteRegistration(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}