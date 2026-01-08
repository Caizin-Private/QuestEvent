package com.questevent.controller;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.enums.Role;
import com.questevent.service.UserWalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserWalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserWalletService userWalletService;

    @InjectMocks
    private UserWalletController userWalletController;

    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(userWalletController)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        closeable.close();
    }

    @Test
    void getMyWalletBalance_success() throws Exception {
        // Mock Security Context
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", Role.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID walletId = UUID.randomUUID();

        UserWalletBalanceDTO dto = new UserWalletBalanceDTO();
        dto.setWalletId(walletId);
        dto.setGems(150);

        when(userWalletService.getMyWalletBalance())
                .thenReturn(dto);

        mockMvc.perform(get("/api/users/me/wallet")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.gems").value(150));
    }

    @Test
    void getMyWalletBalance_userNotFound() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", Role.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userWalletService.getMyWalletBalance())
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        mockMvc.perform(get("/api/users/me/wallet"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyWalletBalance_walletNotFound() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "test@example.com", Role.USER);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userWalletService.getMyWalletBalance())
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        mockMvc.perform(get("/api/users/me/wallet"))
                .andExpect(status().isNotFound());
    }
}
