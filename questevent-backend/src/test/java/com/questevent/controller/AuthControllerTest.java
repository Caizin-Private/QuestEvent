package com.questevent.controller;

import com.questevent.config.JwtAuthFilter;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldShowLoginPageWhenNotLoggedIn() throws Exception {

        when(authService.isLoggedIn(any())).thenReturn(false);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login with Microsoft")));
    }

    @Test
    void shouldRedirectToProfileIfAlreadyLoggedIn() throws Exception {

        when(authService.isLoggedIn(any())).thenReturn(true);

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    @WithMockUser
    void shouldReturnProfilePage() throws Exception {

        User user = new User();
        user.setUserId(1L);
        user.setName("test user");
        user.setEmail("test@example.com");
        user.setGender("MALE");
        user.setDepartment(Department.GENERAL);
        user.setRole(Role.USER);

        when(authService.getLoggedInUser(any())).thenReturn(user);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER")));
    }

    @Test
    @WithMockUser
    void shouldSaveProfileAndRedirect() throws Exception {

        when(authService.getLoggedInUserId(any())).thenReturn(1L);
        doNothing().when(authService).completeProfile(eq(1L), eq(Department.IT), eq("MALE"));

        mockMvc.perform(post("/complete-profile")
                        .param("department", "IT")
                        .param("gender", "MALE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void shouldShowLogoutSuccessPage() throws Exception {

        mockMvc.perform(get("/logout-success"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Logged out successfully")));
    }
}
