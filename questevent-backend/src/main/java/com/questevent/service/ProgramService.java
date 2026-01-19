package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import com.questevent.exception.*;
import com.questevent.repository.*;
import com.questevent.utils.SecurityUserResolver;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
    private final SecurityUserResolver securityUserResolver; // ✅ added

    @Autowired
    public ProgramService(
            ProgramRepository programRepository,
            UserRepository userRepository,
            JudgeRepository judgeRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            SecurityUserResolver securityUserResolver
    ) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.judgeRepository = judgeRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.securityUserResolver = securityUserResolver;
    }

    @Transactional
    public Program createProgram(ProgramRequestDTO dto) {

        log.debug("Create program requested");

        User hostUser =
                securityUserResolver.getCurrentUser();

        User judgeUser = userRepository.findById(dto.getJudgeUserId())
                .orElseThrow(() ->
                        new UserNotFoundException("Judge user not found")
                );

        if (hostUser.getUserId().equals(judgeUser.getUserId())) {
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

        return programRepository.save(program);
    }

    @Transactional
    public Program updateProgram(UUID programId, ProgramRequestDTO dto) {

        User hostUser = securityUserResolver.getCurrentUser();

        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() ->
                        new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE)
                );

        if (!existingProgram.getUser().getUserId().equals(hostUser.getUserId())) {
            throw new AccessDeniedException(
                    "You do not have permission to update this program"
            );
        }

        if (dto.getProgramTitle() != null)
            existingProgram.setProgramTitle(dto.getProgramTitle());
        if (dto.getProgramDescription() != null)
            existingProgram.setProgramDescription(dto.getProgramDescription());
        if (dto.getDepartment() != null)
            existingProgram.setDepartment(dto.getDepartment());
        if (dto.getStartDate() != null)
            existingProgram.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null)
            existingProgram.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null)
            existingProgram.setStatus(dto.getStatus());

        if (dto.getJudgeUserId() != null) {
            User judgeUser = userRepository.findById(dto.getJudgeUserId())
                    .orElseThrow(() ->
                            new UserNotFoundException("Judge user not found")
                    );

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

        return programRepository.save(existingProgram);
    }

    public List<Program> getMyPrograms() {
        return programRepository.findByUser_UserId(
                securityUserResolver.getCurrentUser().getUserId()
        );
    }

    public Program getProgramById(UUID programId) {
        return programRepository.findById(programId)
                .orElseThrow(() ->
                        new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE)
                );
    }

    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public void deleteProgram(UUID programId) {

        User user = securityUserResolver.getCurrentUser();

        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE)
                );

        if (!program.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this program"
            );
        }

        programRepository.delete(program);
    }

    public List<Program> getCompletedProgramsForUser() {

        Long userId = securityUserResolver.getCurrentUser().getUserId();

        List<ProgramRegistration> registrations =
                programRegistrationRepository.findByUserUserId(userId);

        return registrations.stream()
                .map(ProgramRegistration::getProgram)
                .filter(p -> p.getStatus() == ProgramStatus.COMPLETED)
                .toList();
    }

    public List<Program> getProgramsWhereUserIsJudge() {
        return programRepository.findByJudgeUserId(
                securityUserResolver.getCurrentUser().getUserId()
        );
    }

    public List<Program> getActiveProgramsByUserDepartment() {

        User user = userRepository.findById(
                securityUserResolver.getCurrentUser().getUserId()
        ).orElseThrow(() ->
                new UserNotFoundException("User not found")
        );

        return programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE,
                user.getDepartment()
        );
    }

    public List<Program> getDraftProgramsByHost() {
        return programRepository.findByStatusAndUser_UserId(
                ProgramStatus.DRAFT,
                securityUserResolver.getCurrentUser().getUserId()
        );
    }

    public Program changeProgramStatusToActive(UUID programId) {

        User user = securityUserResolver.getCurrentUser();
        Program program = programRepository.findById(programId)
                .orElseThrow(() ->
                        new ProgramNotFoundException(PROGRAM_NOT_FOUND_MESSAGE)
                );

        if (!program.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException(
                    "You do not have permission to change status of this program"
            );
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
}
