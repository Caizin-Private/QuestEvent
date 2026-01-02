package com.questevent.controller;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.service.ProgramRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-registrations")
@RequiredArgsConstructor
public class ProgramRegistrationController {

    private final ProgramRegistrationService programRegistrationService;

    @PostMapping
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

    @GetMapping
    public ResponseEntity<List<ProgramRegistrationDTO>> getAllRegistrations() {
        List<ProgramRegistrationDTO> registrations = 
                programRegistrationService.getAllRegistrations();
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramRegistrationDTO> getRegistrationById(@PathVariable Long id) {
        try {
            ProgramRegistrationDTO registration = 
                    programRegistrationService.getRegistrationById(id);
            return ResponseEntity.ok(registration);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/programs/{programId}")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByProgram(
            @PathVariable Long programId) {
        List<ProgramRegistrationDTO> registrations = 
                programRegistrationService.getRegistrationsByProgramId(programId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ProgramRegistrationDTO>> getRegistrationsByUser(
            @PathVariable Long userId) {
        List<ProgramRegistrationDTO> registrations = 
                programRegistrationService.getRegistrationsByUserId(userId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/programs/{programId}/count")
    public ResponseEntity<Long> getParticipantCount(@PathVariable Long programId) {
        long count = programRegistrationService.getParticipantCountForProgram(programId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {
        try {
            programRegistrationService.deleteRegistration(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}