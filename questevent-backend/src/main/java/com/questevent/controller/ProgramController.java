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

    @PreAuthorize("isAuthenticated()")
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
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get all my hosted programs", description = "Retrieves all programs hosted by a specific user")
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

    //
    @PreAuthorize("isAuthenticated()")
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

    @PreAuthorize("isAuthenticated()")
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

    @PreAuthorize("isAuthenticated()")
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

    private ProgramResponseDTO convertToResponseDTO(Program program) {
        ProgramResponseDTO response = new ProgramResponseDTO();
        response.setProgramId(program.getProgramId());
        response.setProgramTitle(program.getProgramTitle());
        response.setProgramDescription(program.getProgramDescription());
        response.setDepartment(program.getDepartment());
        response.setStartDate(program.getStartDate());
        response.setEndDate(program.getEndDate());
        response.setRegistrationFee(program.getRegistrationFee());
        response.setStatus(program.getStatus());
        response.setHostUserId(program.getUser().getUserId());
        return response;
    }
}
