package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.JudgeRepository;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class ProgramService {

    private static final String PROGRAM_NOT_FOUND_MSG = "Program not found";
    private static final String USER_NOT_FOUND_MSG = "User not found";

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final JudgeRepository judgeRepository;
    private final ProgramRegistrationRepository programRegistrationRepository;

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
        User hostUser = getCurrentUserOrThrow();

        User judgeUser = userRepository.findById(dto.getJudgeUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, USER_NOT_FOUND_MSG));

        if (hostUser.getUserId().equals(judgeUser.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Creator cannot be the judge"
            );
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
        return programRepository.save(program);
    }

    @Transactional
    public Program updateProgram(Long programId, ProgramRequestDTO dto) {
        User hostUser = getCurrentUserOrThrow();

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, PROGRAM_NOT_FOUND_MSG));

        if (!program.getUser().getUserId().equals(hostUser.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to update this program"
            );
        }

        mapDtoToEntity(dto, program);
        return programRepository.save(program);
    }

    public Program getProgramById(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, PROGRAM_NOT_FOUND_MSG));
    }

    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public List<Program> getMyPrograms() {
        User user = getCurrentUserOrThrow();
        return programRepository.findByUser_UserId(user.getUserId());
    }

    public List<Program> getProgramsWhereUserIsJudge() {
        User user = getCurrentUserOrThrow();
        return programRepository.findByJudgeUserId(user.getUserId());
    }

    public List<Program> getActiveProgramsByUserDepartment() {
        User user = getCurrentUserOrThrow();
        return programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE,
                user.getDepartment()
        );
    }

    public List<Program> getDraftProgramsByHost() {
        User user = getCurrentUserOrThrow();
        return programRepository.findByStatusAndUser_UserId(
                ProgramStatus.DRAFT,
                user.getUserId()
        );
    }

    @Transactional
    public Program changeProgramStatusToActive(Long programId) {
        User user = getCurrentUserOrThrow();

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, PROGRAM_NOT_FOUND_MSG));

        if (!program.getUser().getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to change status of this program"
            );
        }

        if (program.getStatus() != ProgramStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Program status must be DRAFT to change to ACTIVE"
            );
        }

        program.setStatus(ProgramStatus.ACTIVE);
        return programRepository.save(program);
    }

    public void deleteProgram(Long programId) {
        User user = getCurrentUserOrThrow();

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, PROGRAM_NOT_FOUND_MSG));

        if (!program.getUser().getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to delete this program"
            );
        }

        programRepository.delete(program);
    }

    public List<Program> getCompletedProgramsForUser() {
        User user = getCurrentUserOrThrow();

        return programRegistrationRepository.findByUserUserId(user.getUserId())
                .stream()
                .map(ProgramRegistration::getProgram)
                .filter(p -> p.getStatus() == ProgramStatus.COMPLETED)
                .toList();
    }

    private User getCurrentUserOrThrow() {
        User user = getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(NOT_FOUND, USER_NOT_FOUND_MSG);
        }
        return user;
    }

    private @Nullable User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal p) {
            return userRepository.findById(p.userId()).orElse(null);
        }
        return null;
    }

    private void mapDtoToEntity(ProgramRequestDTO dto, Program program) {
        program.setProgramTitle(dto.getProgramTitle());
        program.setProgramDescription(dto.getProgramDescription());
        program.setDepartment(dto.getDepartment());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setStatus(dto.getStatus());
    }
}
