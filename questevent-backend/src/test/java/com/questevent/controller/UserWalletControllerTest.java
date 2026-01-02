package com.questevent.controller;

import com.questevent.dto.UserWalletBalanceDto;
import com.questevent.service.UserWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userWalletController).build();
    }

    @Test
    void getWalletBalance_success() throws Exception {
        UUID walletId = UUID.randomUUID();

        UserWalletBalanceDto dto = new UserWalletBalanceDto();
        dto.setWalletId(walletId);
        dto.setGems(150);

        when(userWalletService.getWalletBalance(1L))
                .thenReturn(dto);

        mockMvc.perform(get("/users/{userId}/wallet", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.gems").value(150));
    }

    @Test
    void getWalletBalance_userNotFound() throws Exception {
        when(userWalletService.getWalletBalance(1L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        mockMvc.perform(get("/users/{userId}/wallet", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWalletBalance_walletNotFound() throws Exception {
        when(userWalletService.getWalletBalance(1L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        mockMvc.perform(get("/users/{userId}/wallet", 1L))
                .andExpect(status().isNotFound());
    }
}
