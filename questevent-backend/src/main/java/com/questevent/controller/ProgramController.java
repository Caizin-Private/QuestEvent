package com.questevent.controller;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.ProgramResponseDTO;
import com.questevent.entity.Program;
import com.questevent.service.ProgramService;
import com.questevent.service.ProgramWalletService;
import com.questevent.service.ProgramWalletTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@Tag(name = "Programs", description = "Program management APIs")
public class ProgramController {

    private final ProgramService programService;
    private final ProgramWalletTransactionService programWalletTransactionService;


    @Autowired
    public ProgramController(ProgramService programService, ProgramWalletTransactionService programWalletTransactionService) {
        this.programService = programService;
        this.programWalletTransactionService = programWalletTransactionService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @Operation(
            summary = "Create a new program",
            description = "Creates a new program with the provided details"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Program created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ProgramResponseDTO> createProgram(
            @RequestBody ProgramRequestDTO dto
    ) {
        Program created = programService.createProgram(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertToResponseDTO(created));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all programs", description = "Retrieves a list of all programs")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of programs")
    public ResponseEntity<List<ProgramResponseDTO>> getAllPrograms() {
        List<Program> programs = programService.getAllPrograms();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated() and @rbac.canViewProgram(authentication, #programId)")
    @GetMapping("/{programId}")
    @Operation(summary = "Get program by ID", description = "Retrieves a specific program by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program found"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<ProgramResponseDTO> getProgramById(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        Program program = programService.getProgramById(programId);
        return ResponseEntity.ok(convertToResponseDTO(program));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-programs")
    @Operation(summary = "Get all my hosted programs", description = "Retrieves all programs hosted by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programs"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ProgramResponseDTO>> getMyPrograms() {
        return ResponseEntity.ok(
                programService.getMyPrograms()
                        .stream()
                        .map(this::convertToResponseDTO)
                        .toList()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-completed-registrations")
    @Operation(
            summary = "Get completed programs where user is registered",
            description = "Retrieves all programs where the user has registered and program status is COMPLETED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programs"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ProgramResponseDTO>> getCompletedProgramsForUser() {
        List<Program> programs = programService.getCompletedProgramsForUser();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-judge-programs")
    @Operation(
            summary = "Get programs where user is assigned as judge",
            description = "Retrieves all programs where the current user is assigned as a judge"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programs"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ProgramResponseDTO>> getProgramsWhereUserIsJudge() {
        List<Program> programs = programService.getProgramsWhereUserIsJudge();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("isAuthenticated() and @rbac.canManageProgram(authentication, #programId)")
    @PutMapping("/{programId}")
    @Operation(summary = "Update program", description = "Updates an existing program's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program updated successfully"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId,
            @RequestBody ProgramRequestDTO dto) {
        Program updated = programService.updateProgram(programId, dto);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @PreAuthorize("isAuthenticated() and @rbac.canManageProgram(authentication, #programId)")
    @PostMapping("/{programId}/settle")
    @Operation(summary = "Settle program wallets", description = "Settles all program wallets for a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program wallets settled successfully"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<String> settleProgram(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        programWalletTransactionService.manuallySettleExpiredProgramWallets(programId);
        return ResponseEntity.ok("Program settled successfully");
    }

    @PreAuthorize("isAuthenticated() and @rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{programId}")
    @Operation(summary = "Delete program", description = "Deletes a program from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Program deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<Void> deleteProgram(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        programService.deleteProgram(programId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active-by-department")
    @Operation(
            summary = "Get active programs by user's department",
            description = "Retrieves all programs with ACTIVE status that match the user's department"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programs"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ProgramResponseDTO>> getActiveProgramsByUserDepartment() {
        List<Program> programs = programService.getActiveProgramsByUserDepartment();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-draft-programs")
    @Operation(
            summary = "Get draft programs hosted by user",
            description = "Retrieves all programs with DRAFT status that are hosted by the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programs"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<ProgramResponseDTO>> getDraftProgramsByHost() {
        List<Program> programs = programService.getDraftProgramsByHost();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{programId}/status-to-active")
    @Operation(
            summary = "Change program status from ACTIVE to DRAFT",
            description = "Changes the status of a program from ACTIVE to DRAFT. Only works for programs hosted by the current user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program status changed successfully"),
            @ApiResponse(responseCode = "400", description = "Program status must be ACTIVE"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program or user not found")
    })
    public ResponseEntity<ProgramResponseDTO> changeProgramStatusToActive(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId) {
        Program updated = programService.changeProgramStatusToActive(programId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    private ProgramResponseDTO convertToResponseDTO(Program program) {
        ProgramResponseDTO response = new ProgramResponseDTO();
        response.setProgramId(program.getProgramId());
        response.setProgramTitle(program.getProgramTitle());
        response.setProgramDescription(program.getProgramDescription());
        response.setDepartment(program.getDepartment());
        response.setStartDate(program.getStartDate());
        response.setEndDate(program.getEndDate());
        response.setStatus(program.getStatus());
        response.setHostUserId(program.getUser().getUserId());
        if (program.getJudge() != null && program.getJudge().getUser() != null) {
            response.setJudgeUserId(program.getJudge().getUser().getUserId());
        }
        return response;
    }
}
