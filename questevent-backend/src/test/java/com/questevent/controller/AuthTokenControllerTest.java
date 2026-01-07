package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthTokenController.class)
@AutoConfigureMockMvc(addFilters = false) // security filters off
class AuthTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private Authentication authentication;

    @BeforeEach
    void setup() {
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", Role.USER);
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthInfo() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGenerateTokens() throws Exception {

        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        mockMvc.perform(get("/api/auth/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {

        when(jwtService.validateToken("refresh123")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh123")).thenReturn(true);
        when(jwtService.extractUsername("refresh123")).thenReturn("test@example.com");

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(any())).thenReturn("new-access");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "refresh123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {

        when(jwtService.validateToken("bad")).thenReturn(false);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "bad" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void shouldTestJwtEndpoint() throws Exception {

        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("JWT authentication working ✅"));
    }

    @Test
    void shouldVerifyAuthSource() throws Exception {

        mockMvc.perform(get("/api/auth/verify")
                        .requestAttr("AUTH_SOURCE", "JWT")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationSource").value("JWT"))
                .andExpect(jsonPath("$.hasAuthorizationHeader").value(true));
    }
}
