package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private User user;
    private UserPrincipal principal;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("test@mail.com");
        user.setRole(Role.USER);

        principal = new UserPrincipal(1L, "test@mail.com", Role.USER);

        auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void authInfo_shouldReturnApiInfo() throws Exception {
        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("JWT Authentication API"));
    }

    @Test
    void generateTokens_withAuth_shouldReturnTokens() throws Exception {

        Mockito.when(jwtService.generateAccessToken(Mockito.any()))
                .thenReturn("access-token");
        Mockito.when(jwtService.generateRefreshToken(Mockito.any()))
                .thenReturn("refresh-token");

        mockMvc.perform(get("/api/auth/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void refreshToken_missingToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_invalidToken_shouldReturn401() throws Exception {

        Mockito.when(jwtService.validateToken("bad")).thenReturn(false);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "bad" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyAuthSource_withAttributeAndHeader_shouldReturnSource() throws Exception {

        mockMvc.perform(get("/api/auth/verify")
                        .requestAttr("AUTH_SOURCE", "JWT")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationSource").value("JWT"))
                .andExpect(jsonPath("$.hasAuthorizationHeader").value(true));
    }

    @Test
    void verifyAuthSource_withoutAttribute_shouldReturnUnknown() throws Exception {

        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationSource").value("UNKNOWN"))
                .andExpect(jsonPath("$.hasAuthorizationHeader").value(false));
    }

    @Test
    void generateTokens_withInvalidPrincipal_shouldFail() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("invalid", null)
        );

        mockMvc.perform(get("/api/auth/token"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void refreshToken_validToken_userNotFound_shouldReturn500() throws Exception {

        Mockito.when(jwtService.validateToken("good")).thenReturn(true);
        Mockito.when(jwtService.isRefreshToken("good")).thenReturn(true);
        Mockito.when(jwtService.extractUsername("good"))
                .thenReturn("missing@mail.com");

        Mockito.when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "refreshToken": "good" }
                            """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getCurrentUser_withUserDetailsPrincipal_shouldResolveUser() throws Exception {

        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "test@mail.com",
                        "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null)
        );

        Mockito.when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void verifyAuthSource_withAuthSourceHeader_shouldReturnSource() throws Exception {

        mockMvc.perform(get("/api/auth/verify")
                        .requestAttr("AUTH_SOURCE", "JWT")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationSource").value("JWT"))
                .andExpect(jsonPath("$.hasAuthorizationHeader").value(true));
    }

    @Test
    void verifyAuthSource_withoutAuthSource_shouldReturnUnknown() throws Exception {

        mockMvc.perform(get("/api/auth/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationSource").value("UNKNOWN"))
                .andExpect(jsonPath("$.hasAuthorizationHeader").value(false));
    }


    @Test
    void getCurrentUser_userDetailsButUserNotFound_shouldFail() throws Exception {

        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "missing@mail.com",
                        "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null)
        );

        Mockito.when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generateTokens_withUnsupportedPrincipal_shouldFail() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("STRING_PRINCIPAL", null)
        );

        mockMvc.perform(get("/api/auth/token"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void refreshToken_validTokenButUserMissing_shouldFail() throws Exception {

        Mockito.when(jwtService.validateToken("good")).thenReturn(true);
        Mockito.when(jwtService.isRefreshToken("good")).thenReturn(true);
        Mockito.when(jwtService.extractUsername("good"))
                .thenReturn("missing@mail.com");

        Mockito.when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "refreshToken": "good" }
                            """))
                .andExpect(status().isInternalServerError());
    }








    @Test
    void refreshToken_valid_shouldReturnNewAccessToken() throws Exception {

        Mockito.when(jwtService.validateToken("good")).thenReturn(true);
        Mockito.when(jwtService.isRefreshToken("good")).thenReturn(true);
        Mockito.when(jwtService.extractUsername("good"))
                .thenReturn("test@mail.com");

        Mockito.when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(jwtService.generateAccessToken(Mockito.any()))
                .thenReturn("new-access");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "refreshToken": "good" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void getCurrentUser_withAuth_shouldReturnUserInfo() throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testJwt_withAuth_shouldWork() throws Exception {

        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("JWT authentication working ✅"));
    }
}
