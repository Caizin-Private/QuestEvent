package com.questevent.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.ProgramResponseDTO;
import com.questevent.entity.Program;
import com.questevent.service.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/programs")
public class ProgramController {

    private final ProgramService programService;

    @Autowired
    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(
            @PathVariable Long userId,
            @RequestBody ProgramRequestDTO dto) {
        Program created = programService.createProgram(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDTO(created));
    }

    @PutMapping("/{programId}")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable Long userId,
            @PathVariable Long programId,
            @RequestBody ProgramRequestDTO dto) {
        Program updated = programService.updateProgram(userId, programId, dto);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    @GetMapping
    public ResponseEntity<List<ProgramResponseDTO>> getAllProgramsByUserId(@PathVariable Long userId) {
        List<Program> programs =programService.getProgramsByUserId(userId);
        // Map list of entities to list of DTOs
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    // 2. GET /programs -> Get every program in the system (requires a separate mapping)
// Note: You can place this in the same controller or a more general one
    @GetMapping("/all")
    public ResponseEntity<List<ProgramResponseDTO>> getAllPrograms() {
        List<Program> programs = programService.getAllPrograms();
        List<ProgramResponseDTO> response = programs.stream()
                .map(this::convertToResponseDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{programId}")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable Long userId,
            @PathVariable Long programId
    ) {
        programService.deleteProgram(userId, programId);
        return ResponseEntity.noContent().build();
    }

    // Simple helper to map Entity to DTO
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
