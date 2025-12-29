package com.questevent.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/")
    public String index() {
        return "Welcome! <a href='/oauth2/authorization/azure'>Login with Microsoft</a>";
    }

    @GetMapping("/home")
    public String home() {
        return "Login Successful. You are allowed!";
    }

    @GetMapping("/access-denied")
    public String denied() {
        return "Access denied. Your email is not allowed.";
    }
}