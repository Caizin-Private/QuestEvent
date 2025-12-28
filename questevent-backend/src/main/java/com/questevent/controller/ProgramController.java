package com.questevent.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.entity.Program;
import com.questevent.service.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users/{userId}/programs")
public class ProgramController {

    private final ProgramService programService;

    @Autowired
    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    public ResponseEntity<Program> createProgram(
            @PathVariable Long userId,
            @RequestBody Program program
    ) {
        Program created = programService.createProgram(userId, program);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

