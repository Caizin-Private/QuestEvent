package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
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

    @Autowired
    public ProgramService(ProgramRepository programRepository, UserRepository userRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
    }

    /**
     * CREATE a new program linked to a specific user
     */
    public Program createProgram(Long userId, ProgramRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Program program = new Program();
        mapDtoToEntity(dto, program); // Transfer data from DTO to new Entity
        program.setUser(user);        // Establish relationship
        return programRepository.save(program);
    }

    /**
     * UPDATE an existing program after verifying user ownership
     */
    public Program updateProgram(Long userId, Long programId, ProgramRequestDTO dto) {
        // 1. Find the existing program
        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        // 2. Verify that the user in the URL path is actually the owner of this program
        if (!existingProgram.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this program");
        }

        // 3. Update the fields from the DTO
        mapDtoToEntity(dto, existingProgram);

        // 4. Save changes
        return programRepository.save(existingProgram);
    }

    /**
     * Helper method to map DTO fields to the Entity
     * This avoids code duplication between create and update
     */
    private void mapDtoToEntity(ProgramRequestDTO dto, Program program) {
        program.setProgramTitle(dto.getProgramTitle());
        program.setProgramDescription(dto.getProgramDescription());
        program.setDepartment(dto.getDepartment());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setRegistrationFee(dto.getRegistrationFee());
        program.setStatus(dto.getStatus());
    }

    /**
     * Fetch all programs belonging to a specific user ID
     */
    public List<Program> getProgramsByUserId(Long userId) {
        // Verify user exists first
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return programRepository.findByUser_UserId(userId);
    }

    /**
     * Fetch every program in the database
     */
    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    /**
     * DELETE a program after verifying existence and ownership
     */
    public void deleteProgram(Long userId, Long programId) {
        // 1. Find the program
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        // 2. Ownership Check: Only the host can delete their own program
        if (!program.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this program");
        }

        // 3. Perform deletion
        programRepository.delete(program);
    }
}