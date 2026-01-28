package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.CompleteProfileRequest;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
import com.questevent.rbac.RbacService;
import com.questevent.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // REQUIRED for @PreAuthorize("@rbac...")
    @MockBean
    private RbacService rbacService;

    /* ===================== Helpers ===================== */

    private User mockUser() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);
        return user;
    }

    private UserResponseDto mockDto() {
        return new UserResponseDto(
                1L,
                "Test User",
                "user@test.com",
                Department.IT,
                "MALE",
                Role.USER
        );
    }

    /* ===================== CREATE USER ===================== */

    @Test
    @WithMockUser
    void createUser_ownerAllowed_returns201() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(userService.addUser(any())).thenReturn(mockUser());
        when(userService.convertToDto(any())).thenReturn(mockDto());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(userService).addUser(any());
    }

    /* ===================== GET ALL USERS ===================== */

    @Test
    @WithMockUser
    void getAllUsers_ownerAllowed_returns200() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of(mockDto()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    /* ===================== GET USER BY ID ===================== */

    @Test
    @WithMockUser
    void getUserById_ownerAllowed_returns200() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(userService.getUserById(1L)).thenReturn(mockDto());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    /* ===================== GET CURRENT USER ===================== */

    @Test
    @WithMockUser
    void getCurrentUser_authenticated_returns200() throws Exception {
        when(userService.getCurrentUser()).thenReturn(mockDto());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    /* ===================== UPDATE CURRENT USER ===================== */

    @Test
    @WithMockUser
    void updateCurrentUser_authenticated_returns200() throws Exception {
        when(userService.updateCurrentUser(any())).thenReturn(mockUser());
        when(userService.convertToDto(any())).thenReturn(mockDto());

        mockMvc.perform(put("/api/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(userService).updateCurrentUser(any());
    }

    /* ===================== COMPLETE PROFILE ===================== */

    @Test
    @WithMockUser
    void completeProfile_authenticated_returns200() throws Exception {
        CompleteProfileRequest request =
                new CompleteProfileRequest(Department.IT, "Male");

        when(userService.completeProfile(any())).thenReturn(mockUser());
        when(userService.convertToDto(any())).thenReturn(mockDto());

        mockMvc.perform(post("/api/users/me/complete-profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    /* ===================== DELETE USER ===================== */

    @Test
    @WithMockUser
    void deleteUser_ownerAllowed_returns204() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);

        mockMvc.perform(delete("/api/users/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    /* ===================== SECURITY ===================== */

    @Test
    void unauthenticated_accessDenied() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
