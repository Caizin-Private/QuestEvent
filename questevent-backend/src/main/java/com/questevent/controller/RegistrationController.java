package com.questevent.controller;

import com.questevent.service.RegistrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PreAuthorize("@rbac.canRegisterForProgram(authentication, #programId, #userId)")
    @PostMapping("/program/{programId}")
    public void registerForProgram(
            @PathVariable Long programId,Long userId
    ) {
        registrationService.registerForProgram(programId, userId);
    }

    @PreAuthorize("@rbac.canRegisterForActivity(authentication, #activityId, #userId)")
    @PostMapping("/activity/{activityId}")
    public void registerForActivity(
            @PathVariable Long activityId,Long userId
    ) {
        registrationService.registerForActivity(activityId, userId);
    }
}
