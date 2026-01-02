package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/")
    @ResponseBody
    public String index() {
        return "Welcome! <a href='/oauth2/authorization/azure'>Login with Microsoft</a>";
    }

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



    @PostMapping("/complete-profile")
    @ResponseBody
    public User saveProfile(HttpServletRequest request,
                            @RequestParam String department,
                            @RequestParam String gender) {

        Long userId = (Long) request.getSession().getAttribute("userId");
        User user = userRepository.findById(userId).orElseThrow();

        user.setDepartment(Department.valueOf(department));
        user.setGender(gender);
        return userRepository.save(user);
    }

    @GetMapping("/home")
    @ResponseBody
    public String home(HttpServletRequest request) {
        Long userId = (Long) request.getSession().getAttribute("userId");
        return "<h2>Login Successful 🎉</h2> UserID = " + userId + "<br><a href='/logout'>Logout</a>";
    }

    @GetMapping("/access-denied")
    @ResponseBody
    public String denied() {
        return "<h2 style='color:red;'>Access Denied </h2>";
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
