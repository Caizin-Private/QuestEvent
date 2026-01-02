package com.questevent.controller;

import com.questevent.dto.ProgramWalletBalanceDto;
import com.questevent.dto.ProgramWalletCreateRequest;
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
        ProgramWalletCreateRequest request = new ProgramWalletCreateRequest();
        request.setUserId(1L);
        request.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setGems(100);

        when(programWalletService.createWallet(1L, 10L))
                .thenReturn(wallet);

        ProgramWalletBalanceDto response =
                programWalletController.createProgramWallet(request).getBody();

        assertNotNull(response);
        assertEquals(wallet.getProgramWalletId(), response.getProgramWalletId());
        assertEquals(100, response.getGems());
    }

    @Test
    void getUserProgramWalletBalances_shouldReturnList() {
        ProgramWalletBalanceDto dto = new ProgramWalletBalanceDto();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setGems(200);

        when(programWalletService.getUserProgramWalletBalances(1L))
                .thenReturn(List.of(dto));

        List<ProgramWalletBalanceDto> result =
                programWalletController.getUserProgramWalletBalances(1L).getBody();

        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getGems());
    }

    @Test
    void getProgramWalletBalance_shouldReturnWallet() {
        UUID walletId = UUID.randomUUID();

        ProgramWalletBalanceDto dto = new ProgramWalletBalanceDto();
        dto.setProgramWalletId(walletId);
        dto.setGems(150);

        when(programWalletService.getWalletBalanceByWalletId(walletId))
                .thenReturn(dto);

        ProgramWalletBalanceDto result =
                programWalletController.getProgramWalletBalance(walletId).getBody();

        assertEquals(walletId, result.getProgramWalletId());
        assertEquals(150, result.getGems());
    }
}
