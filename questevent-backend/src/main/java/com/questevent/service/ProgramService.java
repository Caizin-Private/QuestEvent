// java
package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProgramService(ProgramRepository programRepository, UserRepository userRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
    }

    public Program createProgram(Long userId, Program program) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        program.setUser(user);
        return programRepository.save(program);
    }
}
