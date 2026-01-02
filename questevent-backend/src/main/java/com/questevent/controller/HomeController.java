package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return Map.of(
                "userId", principal.userId(),
                "email", principal.email(),
                "role", principal.role()
        );
    }
}
