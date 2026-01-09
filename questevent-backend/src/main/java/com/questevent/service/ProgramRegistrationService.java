package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
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

        log.debug("Self-registration requested | programId={}", request.getProgramId());

        UserPrincipal principal =
                (UserPrincipal) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found with id: " + userId
                ));

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Program not found with id: " + request.getProgramId()
                ));

        if (programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(
                        program.getProgramId(), userId)) {

            log.warn(
                    "Duplicate self-registration blocked | programId={} | userId={}",
                    program.getProgramId(),
                    userId
            );
            throw new IllegalStateException(
                    "User already registered for this program"
            );
        }

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(Instant.now());

        ProgramRegistration saved =
                programRegistrationRepository.save(registration);

        programWalletService.createWallet(userId, program.getProgramId());

        log.info(
                "User registered for program | registrationId={} | programId={} | userId={}",
                saved.getProgramRegistrationId(),
                program.getProgramId(),
                userId
        );

        return mapToResponseDTO(saved);
    }

    @Transactional
    public ProgramRegistrationResponseDTO addParticipantToProgram(
            Long programId,
            AddParticipantInProgramRequestDTO request) {

        log.debug(
                "Admin adding participant | programId={} | userId={}",
                programId,
                request.getUserId()
        );

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Program not found with id: " + programId
                ));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found with id: " + request.getUserId()
                ));

        if (programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(
                        programId, user.getUserId())) {

            log.warn(
                    "Duplicate participant addition blocked | programId={} | userId={}",
                    programId,
                    user.getUserId()
            );
            throw new IllegalStateException(
                    "User already registered for this program"
            );
        }

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(Instant.now());

        ProgramRegistration saved =
                programRegistrationRepository.save(registration);

        programWalletService.createWallet(user.getUserId(), programId);

        log.info(
                "Participant added to program | registrationId={} | programId={} | userId={}",
                saved.getProgramRegistrationId(),
                programId,
                user.getUserId()
        );

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getAllRegistrations() {
        log.info("Fetching all program registrations");
        return programRegistrationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramRegistrationDTO getRegistrationById(Long id) {

        log.debug("Fetching registration by id | registrationId={}", id);

        ProgramRegistration registration =
                programRegistrationRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Registration not found with id: " + id
                        ));

        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getRegistrationsByProgramId(Long programId) {

        log.debug("Fetching registrations by program | programId={}", programId);

        return programRegistrationRepository
                .findByProgramProgramId(programId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramRegistrationDTO> getRegistrationsByUserId(Long userId) {

        log.debug("Fetching registrations by user | userId={}", userId);

        return programRegistrationRepository
                .findByUserUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void deleteRegistration(Long id) {

        log.debug("Delete registration requested | registrationId={}", id);

        if (!programRegistrationRepository.existsById(id)) {
            throw new NoSuchElementException(
                    "Registration not found with id: " + id
            );
        }

        programRegistrationRepository.deleteById(id);
        log.info("Registration deleted | registrationId={}", id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForProgram(Long programId) {

        long count =
                programRegistrationRepository.countByProgramProgramId(programId);

        log.info(
                "Participant count fetched | programId={} | count={}",
                programId,
                count
        );

        return count;
    }

    @Transactional
    public void deleteRegistrationByProgramAndUser(Long programId, Long userId) {

        log.debug(
                "Delete registration by program and user | programId={} | userId={}",
                programId,
                userId
        );

        ProgramRegistration registration =
                programRegistrationRepository
                        .findByProgramProgramIdAndUserUserId(programId, userId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Registration not found for programId="
                                        + programId + ", userId=" + userId
                        ));

        programRegistrationRepository.delete(registration);

        log.info(
                "Registration deleted | registrationId={} | programId={} | userId={}",
                registration.getProgramRegistrationId(),
                programId,
                userId
        );
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

    private ProgramRegistrationResponseDTO mapToResponseDTO(
            ProgramRegistration registration) {

        ProgramRegistrationResponseDTO dto =
                new ProgramRegistrationResponseDTO();

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
