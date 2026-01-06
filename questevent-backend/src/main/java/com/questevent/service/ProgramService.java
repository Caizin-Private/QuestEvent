package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.repository.JudgeRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final JudgeRepository judgeRepository;
    private final ProgramWalletService programWalletService;

    @Autowired
    public ProgramService(ProgramRepository programRepository, UserRepository userRepository, ProgramWalletService programWalletService , JudgeRepository judgeRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.programWalletService = programWalletService;
        this.judgeRepository = judgeRepository;
    }

    public Program createProgram(Long userId,Long judgeUserId, ProgramRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));

        User judgeUser = userRepository.findById(judgeUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Judge user not found"));


        if (user.getUserId().equals(judgeUser.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Creator cannot be the judge"
            );
        }



        Program program = new Program();
        mapDtoToEntity(dto, program);
        program.setUser(user);


        Judge judge = judgeRepository.findByUserUserId(judgeUserId)
                .orElseGet(() -> {
                    Judge newJudge = new Judge();
                    newJudge.setUser(judgeUser);
                    return newJudge;
                });

        program.setJudge(judge);

        return programRepository.save(program);
    }


    public Program updateProgram(Long userId, Long programId, ProgramRequestDTO dto) {
        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (!existingProgram.getUser().getUserId().equals(userId)) {
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


    public List<Program> getProgramsByUserId(Long userId) {
        // Verify user exists first
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return programRepository.findByUser_UserId(userId);
    }

    public Program getProgramById(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
    }


    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public void deleteProgram(Long userId, Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (!program.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this program");
        }

        programRepository.delete(program);
    }
}