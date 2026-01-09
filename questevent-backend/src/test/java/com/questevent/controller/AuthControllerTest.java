package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import com.questevent.service.JwtService;
import com.questevent.config.JwtAuthFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserWalletRepository userWalletRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setupAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                "test-user", null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldShowLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login with Microsoft")));
    }

    @Test
    void shouldRedirectIfAlreadyLoggedIn() throws Exception {
        mockMvc.perform(get("/login").sessionAttr("userId", 10L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    // ================= COMPLETE PROFILE =================

    @Test
    void shouldShowCompleteProfilePage() throws Exception {
        mockMvc.perform(get("/complete-profile").sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Complete Your Profile")));
    }

    @Test
    void shouldSaveProfileAndCreateWallet() throws Exception {

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@test.com");
        user.setName("Test");
        user.setRole(Role.USER);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userWalletRepository.save(any(UserWallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/complete-profile")
                        .sessionAttr("userId", 1L)
                        .param("department", "TECH")
                        .param("gender", "MALE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void shouldShowProfilePage() throws Exception {

        User user = new User();
        user.setUserId(2L);
        user.setName("User");
        user.setEmail("user@test.com");
        user.setDepartment(Department.GENERAL);
        user.setGender("MALE");
        user.setRole(Role.USER);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/profile").sessionAttr("userId", 2L))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("user@test.com")));
    }

    @Test
    void shouldShowLogoutSuccessPage() throws Exception {
        mockMvc.perform(get("/logout-success"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Logged out successfully")));
    }
}
