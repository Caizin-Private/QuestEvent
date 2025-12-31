package com.questevent.service;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public ProgramRegistrationResponseDTO registerParticipantForProgram(ProgramRegistrationRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found with id: " + request.getProgramId()));

        if (programRegistrationRepository.existsByProgram_ProgramIdAndUser_UserId(
                request.getProgramId(), request.getUserId())) {
            throw new RuntimeException("User already registered for this program");
        }

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(LocalDateTime.now());

        ProgramRegistration savedRegistration = programRegistrationRepository.save(registration);
        programWalletService.createWallet(user.getUserId(), program.getProgramId());

        return mapToResponseDTO(savedRegistration);
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