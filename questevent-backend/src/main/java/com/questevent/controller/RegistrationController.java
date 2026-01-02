package com.questevent.controller;

import com.questevent.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Registrations", description = "Simple registration APIs")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PreAuthorize("@rbac.canRegisterForProgram(authentication, #programId, #userId)")
    @PostMapping("/programs/{programId}")
    @Operation(summary = "Register for program", description = "Registers a user for a program using query parameter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<String> registerForProgram(
            @Parameter(description = "Program ID", required = true) @PathVariable Long programId,
            @Parameter(description = "User ID", required = true) @RequestParam Long userId) {
        registrationService.registerForProgram(programId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully registered for program");
    }

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #activityId, #userId)")
    @PostMapping("/activities/{activityId}")
    @Operation(summary = "Register for activity", description = "Registers a user for an activity using query parameter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<String> registerForActivity(
            @Parameter(description = "Activity ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "User ID", required = true) @RequestParam Long userId) {
        registrationService.registerForActivity(activityId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully registered for activity");
    }
}
