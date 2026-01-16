package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.config.SecurityConfig;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.rbac.RbacService;
import com.questevent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    RbacService rbac;

    @MockBean
    JwtDecoder jwtDecoder;

    Jwt jwtToken() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("email", "test@company.com")
        );
    }

    User user() {
        User u = new User();
        u.setName("Test");
        u.setEmail("test@company.com");
        return u;
    }

    UserResponseDto dto() {
        return new UserResponseDto(
                1L,
                "Test",
                "test@company.com",
                null,
                null,
                null
        );
    }

    @BeforeEach
    void setup() {
        when(jwtDecoder.decode(anyString())).thenReturn(jwtToken());
    }

    @Test
    void getCurrentUser_success() throws Exception {
        when(userService.getCurrentUser(any(Jwt.class))).thenReturn(dto());

        mockMvc.perform(
                        get("/api/users/me")
                                .with(jwt().jwt(jwtToken()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@company.com"));
    }

    @Test
    void updateCurrentUser_success() throws Exception {
        when(userService.updateCurrentUser(any(), any())).thenReturn(user());
        when(userService.convertToDto(any())).thenReturn(dto());

        mockMvc.perform(
                        put("/api/users/me")
                                .with(jwt().jwt(jwtToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_success() throws Exception {
        when(rbac.isPlatformOwner(any())).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of(dto()));

        mockMvc.perform(
                        get("/api/users")
                                .with(jwt().jwt(jwtToken()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_success() throws Exception {
        when(rbac.isPlatformOwner(any())).thenReturn(true);
        when(userService.getUserById(1L)).thenReturn(dto());

        mockMvc.perform(
                        get("/api/users/1")
                                .with(jwt().jwt(jwtToken()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void createUser_success() throws Exception {
        when(rbac.isPlatformOwner(any())).thenReturn(true);
        when(userService.addUser(any())).thenReturn(user());
        when(userService.convertToDto(any())).thenReturn(dto());

        mockMvc.perform(
                        post("/api/users")
                                .with(jwt().jwt(jwtToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user()))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void deleteUser_success() throws Exception {
        when(rbac.isPlatformOwner(any())).thenReturn(true);

        mockMvc.perform(
                        delete("/api/users/1")
                                .with(jwt().jwt(jwtToken()))
                )
                .andExpect(status().isNoContent());
    }
}
