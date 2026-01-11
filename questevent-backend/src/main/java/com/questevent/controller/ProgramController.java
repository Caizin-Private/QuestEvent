package com.questevent.controller;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.ProgramResponseDTO;
import com.questevent.entity.Program;
import com.questevent.service.ProgramService;
import com.questevent.service.ProgramWalletTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(ProgramController.class);

    private final ProgramService programService;
    private final ProgramWalletTransactionService programWalletTransactionService;

    @Autowired
    public ProgramController(
            ProgramService programService,
            ProgramWalletTransactionService programWalletTransactionService
    ) {
        this.programService = programService;
        this.programWalletTransactionService = programWalletTransactionService;
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new program",
            description = "Creates a new program with the provided details"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Program created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(@RequestBody ProgramRequestDTO dto) {
        log.info("Creating new program with title='{}'", dto.getProgramTitle());

        Program created = programService.createProgram(dto);

        log.info("Program created successfully with programId={}", created.getProgramId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertToResponseDTO(created));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all programs", description = "Retrieves a list of all programs")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of programs")
    public ResponseEntity<List<ProgramResponseDTO>> getAllPrograms() {
        log.info("Fetching all programs");

        List<Program> programs = programService.getAllPrograms();
        log.debug("Total programs fetched={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
    }

    @PreAuthorize("isAuthenticated() and @rbac.canViewProgram(authentication, #programId)")
    @GetMapping("/{programId}")
    @Operation(summary = "Get program by ID", description = "Retrieves a specific program by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program found"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<ProgramResponseDTO> getProgramById(@Parameter(description = "Program ID", required = true) @PathVariable UUID programId) {
        log.info("Fetching program with programId={}", programId);

        Program program = programService.getProgramById(programId);

        log.debug("Program fetched: programId={}, status={}",
                program.getProgramId(), program.getStatus());

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
        log.info("Fetching programs hosted by current user");

        List<Program> programs = programService.getMyPrograms();
        log.debug("Hosted programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
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
        log.info("Fetching completed programs for current user");

        List<Program> programs = programService.getCompletedProgramsForUser();
        log.debug("Completed registered programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
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
        log.info("Fetching programs where current user is judge");

        List<Program> programs = programService.getProgramsWhereUserIsJudge();
        log.debug("Judge programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PatchMapping("/{programId}")
    @Operation(summary = "Update program", description = "Updates an existing program's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program updated successfully"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId,
            @RequestBody ProgramRequestDTO dto
    ) {
        log.info("Updating program programId={}", programId);

        Program updated = programService.updateProgram(programId, dto);

        log.info("Program updated successfully programId={}", programId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping("/{programId}/settle")
    @Operation(summary = "Settle program wallets", description = "Settles all program wallets for a specific program")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Program wallets settled successfully"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<String> settleProgram(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId) {
        log.info("Manually settling wallets for programId={}", programId);

        programWalletTransactionService.manuallySettleExpiredProgramWallets(programId);

        log.info("Program wallets settled successfully for programId={}", programId);
        return ResponseEntity.ok("Program settled successfully");
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{programId}")
    @Operation(summary = "Delete program", description = "Deletes a program from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Program deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Program not found")
    })
    public ResponseEntity<Void> deleteProgram(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId) {
        log.warn("Deleting program programId={}", programId);

        programService.deleteProgram(programId);

        log.info("Program deleted successfully programId={}", programId);
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
        log.info("Fetching active programs by user's department");

        List<Program> programs = programService.getActiveProgramsByUserDepartment();
        log.debug("Active department programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
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
        log.info("Fetching draft programs hosted by current user");

        List<Program> programs = programService.getDraftProgramsByHost();
        log.debug("Draft programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
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
            @PathVariable UUID programId
    ) {
        log.info("Changing program status to ACTIVE for programId={}", programId);

        Program updated = programService.changeProgramStatusToActive(programId);

        log.info("Program status updated to ACTIVE programId={}", programId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    private ProgramResponseDTO convertToResponseDTO(Program program) {
        log.debug("Converting Program entity to ProgramResponseDTO programId={}",
                program.getProgramId());

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
