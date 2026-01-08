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
    public ResponseEntity<ProgramResponseDTO> getProgramById(@PathVariable Long programId) {
        log.info("Fetching program with programId={}", programId);

        Program program = programService.getProgramById(programId);

        log.debug("Program fetched: programId={}, status={}",
                program.getProgramId(), program.getStatus());

        return ResponseEntity.ok(convertToResponseDTO(program));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-programs")
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
    public ResponseEntity<List<ProgramResponseDTO>> getProgramsWhereUserIsJudge() {
        log.info("Fetching programs where current user is judge");

        List<Program> programs = programService.getProgramsWhereUserIsJudge();
        log.debug("Judge programs count={}", programs.size());

        return ResponseEntity.ok(
                programs.stream().map(this::convertToResponseDTO).toList()
        );
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PutMapping("/{programId}")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable Long programId,
            @RequestBody ProgramRequestDTO dto
    ) {
        log.info("Updating program programId={}", programId);

        Program updated = programService.updateProgram(programId, dto);

        log.info("Program updated successfully programId={}", programId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @PostMapping("/{programId}/settle")
    public ResponseEntity<String> settleProgram(@PathVariable Long programId) {
        log.info("Manually settling wallets for programId={}", programId);

        programWalletTransactionService.manuallySettleExpiredProgramWallets(programId);

        log.info("Program wallets settled successfully for programId={}", programId);
        return ResponseEntity.ok("Program settled successfully");
    }

    @PreAuthorize("@rbac.canManageProgram(authentication, #programId)")
    @DeleteMapping("/{programId}")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long programId) {
        log.warn("Deleting program programId={}", programId);

        programService.deleteProgram(programId);

        log.info("Program deleted successfully programId={}", programId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active-by-department")
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
    public ResponseEntity<ProgramResponseDTO> changeProgramStatusToActive(
            @PathVariable Long programId
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

        return response;
    }
}
