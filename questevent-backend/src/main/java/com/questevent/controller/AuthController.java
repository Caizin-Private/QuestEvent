package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping({"/", "/login"})
    @ResponseBody
    public String loginPage(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        // ✅ already logged-in user
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
    @GetMapping("/profile")
    @ResponseBody
    public String profile(HttpServletRequest request) {

        Long userId = (Long) request.getSession().getAttribute("userId");
        User user = userRepository.findById(userId).orElseThrow();

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
