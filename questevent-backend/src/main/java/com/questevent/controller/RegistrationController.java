package com.questevent.controller;

import com.questevent.service.RegistrationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/programs/{programId}")
    public void registerForProgram(
            @PathVariable Long programId,Long userId
    ) {
        registrationService.registerForProgram(programId, userId);
    }

    @PostMapping("/activities/{activityId}")
    public void registerForActivity(
            @PathVariable Long activityId,Long userId
    ) {
        registrationService.registerForActivity(activityId, userId);
    }
}
