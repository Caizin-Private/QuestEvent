package com.questevent.controller;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.ProgramResponseDTO;
import com.questevent.entity.Program;
import com.questevent.service.ProgramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
@Tag(name = "Programs", description = "Program management APIs")
public class ProgramController {



    private final ProgramService programService;


    public ProgramController(
            ProgramService programService
    ) {
        this.programService = programService;

    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new program")
    @ApiResponse(responseCode = "201", description = "Program created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(
            @RequestBody ProgramRequestDTO dto) {

        Program created = programService.createProgram(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToResponseDTO(created));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all programs")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved programs")
    public ResponseEntity<List<ProgramResponseDTO>> getAllPrograms() {

        return ResponseEntity.ok(
                programService.getAllPrograms()
                        .stream()
                        .map(this::convertToResponseDTO)
                        .toList()
        );
    }

    @PreAuthorize("isAuthenticated() and @rbac.canViewProgram(authentication, #programId)")
    @GetMapping("/{programId}")
    @Operation(summary = "Get program by ID")
    @ApiResponse(responseCode = "200", description = "Program found")
    @ApiResponse(responseCode = "404", description = "Program not found")
    public ResponseEntity<ProgramResponseDTO> getProgramById(
            @PathVariable UUID programId) {

        return ResponseEntity.ok(
                convertToResponseDTO(programService.getProgramById(programId))
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-programs")
    @Operation(summary = "Get my hosted programs")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved programs")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<List<ProgramResponseDTO>> getMyPrograms() {

        return ResponseEntity.ok(
                programService.getMyPrograms()
                        .stream()
                        .map(this::convertToResponseDTO)
                        .toList()
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PutMapping("/{programId}")
    @Operation(summary = "Update program")
    @ApiResponse(responseCode = "200", description = "Program updated successfully")
    @ApiResponse(responseCode = "403", description = "Permission denied")
    @ApiResponse(responseCode = "404", description = "Program not found")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable UUID programId,
            @RequestBody ProgramRequestDTO dto) {

        return ResponseEntity.ok(
                convertToResponseDTO(programService.updateProgram(programId, dto))
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{programId}")
    @Operation(summary = "Delete program")
    @ApiResponse(responseCode = "204", description = "Program deleted successfully")
    @ApiResponse(responseCode = "403", description = "Permission denied")
    @ApiResponse(responseCode = "404", description = "Program not found")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable UUID programId) {

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
        response.setStatus(program.getStatus());
        response.setHostUserId(program.getUser().getUserId());

        if (program.getJudge() != null && program.getJudge().getUser() != null) {
            response.setJudgeUserId(program.getJudge().getUser().getUserId());
        }

        response.setCreatedAt(program.getCreatedAt());
        response.setUpdatedAt(program.getUpdatedAt());
        return response;
    }
}
