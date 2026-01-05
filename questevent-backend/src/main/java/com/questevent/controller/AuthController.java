package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/")
    @ResponseBody
    public String home(HttpServletRequest request) {

        Long userId = (Long) request.getSession().getAttribute("userId");

        return """
            <h2>Login Successful 🎉</h2>
            <p>UserID = %d</p>

            <form method="POST" action="/logout">
                <button type="submit">Logout</button>
            </form>
        """.formatted(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/complete-profile")
    @ResponseBody
    public Map<String, Object> getProfile(HttpServletRequest request) {

        Long userId = (Long) request.getSession().getAttribute("userId");
        User user = userRepository.findById(userId).orElseThrow();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", user.getUserId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("department", user.getDepartment());
        response.put("gender", user.getGender());
        response.put("role", user.getRole());

        return response;
    }

    @GetMapping("/logout-success")
    @ResponseBody
    public String logoutSuccess() {
        return """
            <h2>Logged out successfully </h2>
            <a href="/login">Login again</a>
        """;
    }
}
