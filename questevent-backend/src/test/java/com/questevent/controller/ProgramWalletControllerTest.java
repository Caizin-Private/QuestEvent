package com.questevent.controller;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.dto.ProgramWalletCreateRequestDTO;
import com.questevent.entity.ProgramWallet;
import com.questevent.service.ProgramWalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramWalletControllerTest {

    @Mock
    private ProgramWalletService programWalletService;

    @InjectMocks
    private ProgramWalletController programWalletController;

    @Test
    void createProgramWallet_shouldReturnWalletBalanceDto() {
        ProgramWalletCreateRequestDTO request = new ProgramWalletCreateRequestDTO();
        request.setUserId(1L);
        request.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setGems(100);

        when(programWalletService.createWallet(1L, 10L))
                .thenReturn(wallet);

        ProgramWalletBalanceDTO response =
                programWalletController.createProgramWallet(request).getBody();

        assertNotNull(response);
        assertEquals(wallet.getProgramWalletId(), response.getProgramWalletId());
        assertEquals(100, response.getGems());
    }

    @Test
    void getUserProgramWalletBalances_shouldReturnList() {
        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setGems(200);

        when(programWalletService.getUserProgramWalletBalances(1L))
                .thenReturn(List.of(dto));

        List<ProgramWalletBalanceDTO> result =
                programWalletController.getUserProgramWalletBalances(1L).getBody();

        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getGems());
    }

    @Test
    void getProgramWalletBalance_shouldReturnWallet() {
        UUID walletId = UUID.randomUUID();

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(walletId);
        dto.setGems(150);

        when(programWalletService.getWalletBalanceByWalletId(walletId))
                .thenReturn(dto);

        ProgramWalletBalanceDTO result =
                programWalletController.getProgramWalletBalance(walletId).getBody();

        assertEquals(walletId, result.getProgramWalletId());
        assertEquals(150, result.getGems());
    }
}
