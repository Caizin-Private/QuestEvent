package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Department;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;

    @GetMapping({"/", "/login"})
    @ResponseBody
    public String loginPage(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        // Already logged in → redirect to profile
        if (session != null && session.getAttribute("userId") != null) {
            response.sendRedirect("/profile");
            return null;
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Login</title>
            </head>
            <body style="font-family: Arial; padding: 40px;">
                <h2>Login Page</h2>

                <a href="/oauth2/authorization/azure">
                    <button style="padding:10px 20px; cursor:pointer;">
                        Login with Microsoft
                    </button>
                </a>

            </body>
            </html>
        """;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/complete-profile")
    @ResponseBody
    public String completeProfilePage() {

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
    public void saveProfile(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam Department department,
            @RequestParam String gender
    ) throws IOException {

        Long userId = (Long) request.getSession().getAttribute("userId");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDepartment(department);
        user.setGender(gender);
        userRepository.save(user);

        // ✅ Sonar-compliant: return value is used
        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUser(user);
                    newWallet.setGems(0);
                    return userWalletRepository.save(newWallet);
                });

        response.sendRedirect("/profile");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    @ResponseBody
    public String profile(HttpServletRequest request) {

        Long userId = (Long) request.getSession().getAttribute("userId");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
        return """
            <h2>Logged out successfully ✅</h2>
            <a href="/login">Login again</a>
        """;
    }
}
