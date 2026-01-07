package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final JudgeRepository judgeRepository;
    private final ProgramWalletService programWalletService;
    private final ProgramRegistrationRepository programRegistrationRepository;

    @Autowired
    public ProgramService(ProgramRepository programRepository, UserRepository userRepository, ProgramWalletService programWalletService, JudgeRepository judgeRepository, ProgramRegistrationRepository programRegistrationRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.programWalletService = programWalletService;
        this.judgeRepository = judgeRepository;
        this.programRegistrationRepository = programRegistrationRepository;
    }

    public Program createProgram(ProgramRequestDTO dto) {

        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User hostUser = null;

        // Reuse RBAC logic style
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                hostUser = userRepository.findById(p.userId()).orElse(null);
            }
        }

        if (hostUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }


        User judgeUser = userRepository.findById(dto.getJudgeUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Judge user not found"
                ));

        if (hostUser.getUserId().equals(judgeUser.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Creator cannot be the judge"
            );
        }

        Program program = new Program();
        mapDtoToEntity(dto, program);
        program.setUser(hostUser);

        Judge judge = judgeRepository
                .findByUserUserId(judgeUser.getUserId())
                .orElseGet(() -> {
                    Judge j = new Judge();
                    j.setUser(judgeUser);
                    return j;
                });

        program.setJudge(judge);

        return programRepository.save(program);
    }


    public Program updateProgram(Long programId, ProgramRequestDTO dto) {

        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User hostUser = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                hostUser = userRepository.findById(p.userId()).orElse(null);
            }
        }

        if (hostUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (!existingProgram.getUser().getUserId().equals(hostUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this program");
        }

        mapDtoToEntity(dto, existingProgram);

        return programRepository.save(existingProgram);
    }


    private void mapDtoToEntity(ProgramRequestDTO dto, Program program) {
        program.setProgramTitle(dto.getProgramTitle());
        program.setProgramDescription(dto.getProgramDescription());
        program.setDepartment(dto.getDepartment());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setRegistrationFee(dto.getRegistrationFee());
        program.setStatus(dto.getStatus());
    }


    public List<Program> getMyPrograms() {
        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User hostUser = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                hostUser = userRepository.findById(p.userId()).orElse(null);
            }
        }

        if (hostUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
        return programRepository.findByUser_UserId(hostUser.getUserId());
    }

    public Program getProgramById(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
    }


    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public void deleteProgram(Long programId) {
        @Nullable Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User hostUser = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal p) {
                hostUser = userRepository.findById(p.userId()).orElse(null);
            }
        }

        if (hostUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (!program.getUser().getUserId().equals(hostUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this program");
        }

        programRepository.delete(program);
    }

    public List<Program> getCompletedProgramsForUser() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        List<ProgramRegistration> registrations = programRegistrationRepository.findByUserUserId(currentUser.getUserId());
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
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
        return programRepository.findByJudgeUserId(currentUser.getUserId());
    }

    public List<Program> getActiveProgramsByUserDepartment() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        return programRepository.findByStatusAndDepartment(ProgramStatus.ACTIVE, currentUser.getDepartment());
    }


    public List<Program> getDraftProgramsByHost() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        return programRepository.findByStatusAndUser_UserId(ProgramStatus.DRAFT, currentUser.getUserId());
    }

    public Program changeProgramStatusToDraft(Long programId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (!program.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to change status of this program");
        }

        if (program.getStatus() != ProgramStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program status must be ACTIVE to change to DRAFT");
        }

        program.setStatus(ProgramStatus.DRAFT);
        return programRepository.save(program);
    }

}