package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramRegistrationService {

    private final ProgramRegistrationRepository programRegistrationRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final ProgramWalletService programWalletService;

    @Transactional
    public ProgramRegistrationResponseDTO registerParticipantForProgram(
            ProgramRegistrationRequestDTO request) {

        UserPrincipal principal =
                (UserPrincipal) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(
                        program.getProgramId(), userId)) {
            throw new RuntimeException("User already registered for this program");
        }

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(Instant.now());

        ProgramRegistration saved =
                programRegistrationRepository.save(registration);

        programWalletService.createWallet(userId, program.getProgramId());

        return mapToResponseDTO(saved);
    }

    @Transactional
    public ProgramRegistrationResponseDTO addParticipantToProgram(
            Long programId,
            AddParticipantInProgramRequestDTO request) {

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(
                        programId, user.getUserId())) {
            throw new RuntimeException("User already registered for this program");
        }

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(Instant.now());

        ProgramRegistration saved =
                programRegistrationRepository.save(registration);

        programWalletService.createWallet(user.getUserId(), programId);

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getAllRegistrations() {
        return programRegistrationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramRegistrationDTO getRegistrationById(Long id) {
        ProgramRegistration registration = programRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found with id: " + id));
        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getRegistrationsByProgramId(Long programId) {
        return programRegistrationRepository.findByProgramProgramId(programId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getRegistrationsByUserId(Long userId) {
        return programRegistrationRepository.findByUserUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRegistration(Long id) {
        if (!programRegistrationRepository.existsById(id)) {
            throw new RuntimeException("Registration not found with id: " + id);
        }
        programRegistrationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForProgram(Long programId) {
        return programRegistrationRepository.countByProgramProgramId(programId);
    }

    @Transactional
    public void deleteRegistrationByProgramAndUser(Long programId, Long userId) {
        ProgramRegistration registration = programRegistrationRepository
                .findByProgramProgramIdAndUserUserId(programId, userId)
                .orElseThrow(() -> new RuntimeException("Registration not found for program id: " + programId + " and user id: " + userId));
        programRegistrationRepository.delete(registration);
    }

    private ProgramRegistrationDTO mapToDTO(ProgramRegistration registration) {
        return new ProgramRegistrationDTO(
                registration.getProgramRegistrationId(),
                registration.getProgram().getProgramId(),
                registration.getProgram().getProgramTitle(),
                registration.getUser().getUserId(),
                registration.getUser().getName(),
                registration.getRegisteredAt()
        );
    }

    private ProgramRegistrationResponseDTO mapToResponseDTO(ProgramRegistration registration) {
        ProgramRegistrationResponseDTO dto = new ProgramRegistrationResponseDTO();
        dto.setProgramRegistrationId(registration.getProgramRegistrationId());
        dto.setProgramId(registration.getProgram().getProgramId());
        dto.setProgramTitle(registration.getProgram().getProgramTitle());
        dto.setUserId(registration.getUser().getUserId());
        dto.setUserName(registration.getUser().getName());
        dto.setUserEmail(registration.getUser().getEmail());
        dto.setRegisteredAt(registration.getRegisteredAt());
        dto.setMessage("Successfully registered for program");
        return dto;
    }
}
