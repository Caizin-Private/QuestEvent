package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    @ResponseBody
    public String loginPage(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {

        log.info("Login page requested");

        if (authService.isLoggedIn(request)) {
            log.info("User already logged in, redirecting to /profile");
            response.sendRedirect("/profile");
            return null;
        }

        return """
        <!DOCTYPE html>
        <html>
        <head><title>Login</title></head>
        <body style="font-family: Arial; padding: 40px;">
            <h2>Login Page</h2>
            <a href="/oauth2/authorization/azure">
                <button style="padding:10px 20px;">Login with Microsoft</button>
            </a>
        </body>
        </html>
        """;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/complete-profile")
    @ResponseBody
    public String completeProfilePage() {

        log.info("Complete profile page opened");
        return """
        <h2>Complete Your Profile 📝</h2>

        <form method="post" action="/complete-profile">

            <label>Department:</label><br>
            <select name="department">
                <option value="HR">HR</option>
                <option value="TECH">TECH</option>
                <option value="GENERAL">GENERAL</option>
                <option value="IT">IT</option>
            </select><br><br>

            <label>Gender:</label><br>
            <select name="gender">
                <option value="MALE">MALE</option>
                <option value="FEMALE">FEMALE</option>
            </select><br><br>

            <button type="submit">Save</button>
        </form>
        """;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/complete-profile")
    public void saveProfile(HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam Department department,
                            @RequestParam String gender) throws IOException {

        log.info("Profile submission received department={}, gender={}",
                department, gender);

        Long userId = authService.getLoggedInUserId(request);
        authService.completeProfile(userId, department, gender);

        log.info("Profile completed successfully, redirecting to /profile");
        response.sendRedirect("/profile");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    @ResponseBody
    public String profile(HttpServletRequest request) {

        log.info("Profile page requested");

        User user = authService.getLoggedInUser(request);

        log.debug("Rendering profile page userId={}", user.getUserId());

        return """
            <h2>User Profile 👤</h2>

            <p><b>User ID:</b> %d</p>
            <p><b>Name:</b> %s</p>
            <p><b>Email:</b> %s</p>
            <p><b>Department:</b> %s</p>
            <p><b>Gender:</b> %s</p>
            <p><b>Role:</b> %s</p>

            <form method="post" action="/logout">
                <button style="padding:8px 15px; margin-top:20px;">
                    Logout
                </button>
            </form>
        """.formatted(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getGender(),
                user.getRole()
        );
    }

    @GetMapping("/logout-success")
    @ResponseBody
    public String logoutSuccess() {
        log.info("Logout success page requested");
        return """
            <h2>Logged out successfully </h2>
            <a href="/">Login again</a>
        """;
    }
}
