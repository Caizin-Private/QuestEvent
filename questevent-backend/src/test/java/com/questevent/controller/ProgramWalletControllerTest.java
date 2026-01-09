package com.questevent.controller;

import com.questevent.dto.ProgramWalletBalanceDTO;
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
    void getProgramWalletBalance_shouldReturnWallet() {
        UUID walletId = UUID.randomUUID();

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(walletId);
        dto.setGems(150);

        when(programWalletService.getWalletBalanceByWalletId(walletId))
                .thenReturn(dto);

        ProgramWalletBalanceDTO result =
                programWalletController.getProgramWalletBalanceByProgramWalletId(walletId).getBody();

        assertNotNull(result);
        assertEquals(walletId, result.getProgramWalletId());
        assertEquals(150, result.getGems());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnList() {
        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setUserId(1L);
        dto.setGems(300);

        when(programWalletService.getProgramWalletsByProgramId(10L))
                .thenReturn(List.of(dto));

        List<ProgramWalletBalanceDTO> result =
                programWalletController.getProgramWalletsByProgramId(10L).getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(300, result.get(0).getGems());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void getProgramWalletsByProgramId_whenNoWallets_shouldReturnEmptyList() {
        when(programWalletService.getProgramWalletsByProgramId(10L))
                .thenReturn(List.of());

        List<ProgramWalletBalanceDTO> result =
                programWalletController.getProgramWalletsByProgramId(10L).getBody();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnMultipleWallets() {
        ProgramWalletBalanceDTO dto1 = new ProgramWalletBalanceDTO();
        dto1.setProgramWalletId(UUID.randomUUID());
        dto1.setUserId(1L);
        dto1.setGems(100);

        ProgramWalletBalanceDTO dto2 = new ProgramWalletBalanceDTO();
        dto2.setProgramWalletId(UUID.randomUUID());
        dto2.setUserId(2L);
        dto2.setGems(200);

        when(programWalletService.getProgramWalletsByProgramId(10L))
                .thenReturn(List.of(dto1, dto2));

        List<ProgramWalletBalanceDTO> result =
                programWalletController.getProgramWalletsByProgramId(10L).getBody();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100, result.get(0).getGems());
        assertEquals(200, result.get(1).getGems());
    }



    @Test
    void getMyProgramWallet_shouldReturnWallet() {
        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setProgramId(9L);
        dto.setUserId(1L);
        dto.setGems(50);

        when(programWalletService.getMyProgramWallet(9L))
                .thenReturn(dto);

        ProgramWalletBalanceDTO result =
                programWalletController.getMyProgramWallet(9L).getBody();

        assertNotNull(result);
        assertEquals(9L, result.getProgramId());
        assertEquals(1L, result.getUserId());
        assertEquals(50, result.getGems());
    }
    @Test
    void getMyProgramWallet_whenWalletNotFound_shouldThrowException() {
        when(programWalletService.getMyProgramWallet(9L))
                .thenThrow(new RuntimeException("Program wallet not found"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programWalletController.getMyProgramWallet(9L)
        );

        assertEquals("Program wallet not found", ex.getMessage());
    }

}
