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

    private static final String USER_NOT_FOUND = "User not found";
    private static final String PROGRAM_NOT_FOUND = "Program not found";

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
                    return new UserNotFoundException(USER_NOT_FOUND);
                });

        User judgeUser = userRepository.findById(dto.getJudgeUserId())
                .orElseThrow(() -> {
                    log.error("Judge user not found | judgeUserId={}", dto.getJudgeUserId());
                    return new UserNotFoundException(USER_NOT_FOUND);
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
                    return j;
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

        log.debug("Update program requested | programId={}", programId);

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
                    return new UserNotFoundException(USER_NOT_FOUND);
                });

        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error("Program not found | programId={}", programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND);
                });

        if (!existingProgram.getUser().getUserId().equals(hostUser.getUserId())) {
            log.warn(
                    "Program update forbidden | programId={} | requesterId={}",
                    programId,
                    hostUser.getUserId()
            );
            throw new AccessDeniedException("You do not have permission to update this program");
        }

        mapDtoToEntity(dto, existingProgram);

        if (dto.getJudgeUserId() != null) {

            User judgeUser = userRepository.findById(dto.getJudgeUserId())
                    .orElseThrow(() -> {
                        log.error("Judge user not found | judgeUserId={}", dto.getJudgeUserId());
                        return new UserNotFoundException(USER_NOT_FOUND);
                    });

            if (hostUser.getUserId().equals(judgeUser.getUserId())) {
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
                "Program updated | programId={} | hostUserId={}",
                updated.getProgramId(),
                hostUser.getUserId()
        );

        return updated;
    }

    public List<Program> getMyPrograms() {
        User hostUser = getCurrentUser();
        if (hostUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        return programRepository.findByUser_UserId(hostUser.getUserId());
    }

    public Program getProgramById(UUID programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(PROGRAM_NOT_FOUND));
    }

    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public void deleteProgram(UUID programId) {

        User hostUser = getCurrentUser();
        if (hostUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(PROGRAM_NOT_FOUND));

        if (!program.getUser().getUserId().equals(hostUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to delete this program");
        }

        programRepository.delete(program);
    }

    public List<Program> getCompletedProgramsForUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }

        return programRegistrationRepository.findByUserUserId(currentUser.getUserId())
                .stream()
                .map(ProgramRegistration::getProgram)
                .filter(p -> p.getStatus() == ProgramStatus.COMPLETED)
                .toList();
    }

    public List<Program> getProgramsWhereUserIsJudge() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        return programRepository.findByJudgeUserId(currentUser.getUserId());
    }

    public List<Program> getActiveProgramsByUserDepartment() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        return programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE,
                currentUser.getDepartment()
        );
    }

    public List<Program> getDraftProgramsByHost() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        return programRepository.findByStatusAndUser_UserId(
                ProgramStatus.DRAFT,
                currentUser.getUserId()
        );
    }

    public Program changeProgramStatusToActive(UUID programId) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(PROGRAM_NOT_FOUND));

        if (!program.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to change status of this program");
        }

        if (program.getStatus() != ProgramStatus.DRAFT) {
            throw new ResourceConflictException(
                    "Program status must be DRAFT to change to ACTIVE"
            );
        }

        program.setStatus(ProgramStatus.ACTIVE);
        return programRepository.save(program);
    }

    private void mapDtoToEntity(ProgramRequestDTO dto, Program program) {
        program.setProgramTitle(dto.getProgramTitle());
        program.setProgramDescription(dto.getProgramDescription());
        program.setDepartment(dto.getDepartment());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setStatus(dto.getStatus());
    }

    private User getCurrentUser() {
        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal p) {
            return userRepository.findById(p.userId()).orElse(null);
        }
        return null;
    }
}
