package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
public class ProgramService {

    private static final String PROGRAM_NOT_FOUND_MESSAGE = "Program not found";
    private static final String LOG_PROGRAM_NOT_FOUND = "Program not found | programId={}";


    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final JudgeRepository judgeRepository;
    private final ProgramRegistrationRepository programRegistrationRepository;

    @Autowired
    public ProgramService(
            ProgramRepository programRepository,
            UserRepository userRepository,
            JudgeRepository judgeRepository,
            ProgramRegistrationRepository programRegistrationRepository) {

        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.judgeRepository = judgeRepository;
        this.programRegistrationRepository = programRegistrationRepository;
    }

    @Transactional
    public Program createProgram(ProgramRequestDTO dto) {

        log.debug("Create program requested");

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal p)) {
            log.warn("Unauthorized program creation attempt");
            throw new UnauthorizedException("Unauthorized");
        }

        User hostUser = userRepository.findById(p.userId())
                .orElseThrow(() -> {
                    log.error("Host user not found | userId={}", p.userId());
                    return new UserNotFoundException("User not found");
                });

        User judgeUser = userRepository.findById(dto.getJudgeUserId())
                .orElseThrow(() -> {
                    log.error("Judge user not found | judgeUserId={}", dto.getJudgeUserId());
                    return new UserNotFoundException("Judge user not found");
                });

        if (hostUser.getUserId().equals(judgeUser.getUserId())) {
            log.warn("Creator attempted to assign self as judge | userId={}", hostUser.getUserId());
            throw new IllegalArgumentException("Creator cannot be the judge");
        }

        Program program = new Program();
        mapDtoToEntity(dto, program);
        program.setUser(hostUser);

        Judge judge = judgeRepository.findByUserUserId(judgeUser.getUserId())
                .orElseGet(() -> {
                    Judge j = new Judge();
                    j.setUser(judgeUser);
                    return j; // persisted via CascadeType.PERSIST
                });

        program.setJudge(judge);

        Program saved = programRepository.save(program);

        log.info(
                "Program created | programId={} | hostUserId={} | judgeUserId={}",
                saved.getProgramId(),
                hostUser.getUserId(),
                judgeUser.getUserId()
        );

        return saved;
    }

    @Transactional
    public Program updateProgram(UUID programId, ProgramRequestDTO dto) {

        log.debug("PATCH update program requested | programId={}", programId);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal p)) {
            log.warn("Unauthorized program update attempt | programId={}", programId);
            throw new UnauthorizedException("Unauthorized");
        }

        User hostUser = userRepository.findById(p.userId())
                .orElseThrow(() -> {
                    log.error("Host user not found | userId={}", p.userId());
                    return new UserNotFoundException("User not found");
                });

        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error(LOG_PROGRAM_NOT_FOUND, programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE);
                });

        if (!existingProgram.getUser().getUserId().equals(hostUser.getUserId())) {
            log.warn(
                    "Program update forbidden | programId={} | requesterId={}",
                    programId,
                    hostUser.getUserId()
            );
            throw new AccessDeniedException("You do not have permission to update this program");
        }


        if (dto.getProgramTitle() != null) {
            existingProgram.setProgramTitle(dto.getProgramTitle());
        }

        if (dto.getProgramDescription() != null) {
            existingProgram.setProgramDescription(dto.getProgramDescription());
        }

        if (dto.getDepartment() != null) {
            existingProgram.setDepartment(dto.getDepartment());
        }

        if (dto.getStartDate() != null) {
            existingProgram.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            existingProgram.setEndDate(dto.getEndDate());
        }

        if (dto.getStatus() != null) {
            existingProgram.setStatus(dto.getStatus());
        }


        if (dto.getJudgeUserId() != null) {

            User judgeUser = userRepository.findById(dto.getJudgeUserId())
                    .orElseThrow(() -> {
                        log.error("Judge user not found | judgeUserId={}", dto.getJudgeUserId());
                        return new UserNotFoundException("Judge user not found");
                    });

            if (hostUser.getUserId().equals(judgeUser.getUserId())) {
                log.warn("Creator attempted to assign self as judge | userId={}", hostUser.getUserId());
                throw new IllegalArgumentException("Creator cannot be the judge");
            }

            Judge judge = judgeRepository.findByUserUserId(judgeUser.getUserId())
                    .orElseGet(() -> {
                        Judge j = new Judge();
                        j.setUser(judgeUser);
                        return j;
                    });

            existingProgram.setJudge(judge);
        }

        Program updated = programRepository.save(existingProgram);

        log.info(
                "Program patched successfully | programId={} | hostUserId={}",
                updated.getProgramId(),
                hostUser.getUserId()
        );

        return updated;
    }


    private void mapDtoToEntity(ProgramRequestDTO dto, Program program) {
        program.setProgramTitle(dto.getProgramTitle());
        program.setProgramDescription(dto.getProgramDescription());
        program.setDepartment(dto.getDepartment());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setStatus(dto.getStatus());
    }

    public List<Program> getMyPrograms() {
        User hostUser = getCurrentUser();
        if (hostUser == null) {
            log.warn("User not found while fetching my programs");
            throw new UserNotFoundException("User not found");
        }

        log.info("Fetching programs created by user | userId={}", hostUser.getUserId());
        return programRepository.findByUserUserId(hostUser.getUserId());
    }

    public Program getProgramById(UUID programId) {
        log.debug("Fetching program by id | programId={}", programId);
        return programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error(LOG_PROGRAM_NOT_FOUND, programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE);
                });
    }

    public List<Program> getAllPrograms() {
        log.info("Fetching all programs");
        return programRepository.findAll();
    }

    public void deleteProgram(UUID programId) {

        log.debug("Delete program requested | programId={}", programId);

        User hostUser = getCurrentUser();
        if (hostUser == null) {
            log.warn("User not found while deleting program | programId={}", programId);
            throw new UserNotFoundException("User not found");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error(LOG_PROGRAM_NOT_FOUND, programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE);
                });

        if (!program.getUser().getUserId().equals(hostUser.getUserId())) {
            log.warn(
                    "Program delete forbidden | programId={} | requesterId={}",
                    programId,
                    hostUser.getUserId()
            );
            throw new AccessDeniedException("You do not have permission to delete this program");
        }

        programRepository.delete(program);

        log.info(
                "Program deleted | programId={} | hostUserId={}",
                programId,
                hostUser.getUserId()
        );
    }

    public List<Program> getCompletedProgramsForUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("User not found while fetching completed programs");
            throw new UserNotFoundException("User not found");
        }

        List<ProgramRegistration> registrations =
                programRegistrationRepository.findByUserUserId(currentUser.getUserId());

        log.info(
                "Fetching completed programs | userId={} | totalRegistrations={}",
                currentUser.getUserId(),
                registrations.size()
        );

        return registrations.stream()
                .map(ProgramRegistration::getProgram)
                .filter(program -> program.getStatus() == ProgramStatus.COMPLETED)
                .toList();
    }

    private User getCurrentUser() {
        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                return userRepository.findById(p.userId()).orElse(null);
            }
        }

        return null;
    }

    public List<Program> getProgramsWhereUserIsJudge() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("User not found while fetching judge programs");
            throw new UserNotFoundException("User not found");
        }

        log.info("Fetching programs where user is judge | userId={}", currentUser.getUserId());
        return programRepository.findByJudgeUserId(currentUser.getUserId());
    }

    public List<Program> getActiveProgramsByUserDepartment() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("User not found while fetching department programs");
            throw new UserNotFoundException("User not found");
        }

        log.info(
                "Fetching active programs by department | department={}",
                currentUser.getDepartment()
        );

        return programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE,
                currentUser.getDepartment()
        );
    }

    public List<Program> getDraftProgramsByHost() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("User not found while fetching draft programs");
            throw new UserNotFoundException("User not found");
        }

        log.info("Fetching draft programs | hostUserId={}", currentUser.getUserId());
        return programRepository.findByStatusAndUserUserId(
                ProgramStatus.DRAFT,
                currentUser.getUserId()
        );
    }

    public Program changeProgramStatusToActive(UUID programId) {

        log.debug("Change program status to ACTIVE requested | programId={}", programId);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("User not found while changing program status | programId={}", programId);
            throw new UserNotFoundException("User not found");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error(LOG_PROGRAM_NOT_FOUND, programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE);
                });

        if (!program.getUser().getUserId().equals(currentUser.getUserId())) {
            log.warn(
                    "Status change forbidden | programId={} | requesterId={}",
                    programId,
                    currentUser.getUserId()
            );
            throw new AccessDeniedException("You do not have permission to change status of this program");
        }

        if (program.getStatus() != ProgramStatus.DRAFT) {
            log.warn(
                    "Invalid status transition | programId={} | currentStatus={}",
                    programId,
                    program.getStatus()
            );
            throw new ResourceConflictException(
                    "Program status must be DRAFT to change to ACTIVE"
            );
        }

        program.setStatus(ProgramStatus.ACTIVE);
        Program updated = programRepository.save(program);

        log.info(
                "Program status changed to ACTIVE | programId={}",
                programId
        );

        return updated;
    }
}
