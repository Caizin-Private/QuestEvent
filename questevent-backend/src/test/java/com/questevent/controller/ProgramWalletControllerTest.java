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
        dto.setGems(150L);

        when(programWalletService.getWalletBalanceByWalletId(walletId))
                .thenReturn(dto);

        ProgramWalletBalanceDTO result =
                programWalletController
                        .getProgramWalletBalanceByProgramWalletId(walletId)
                        .getBody();

        assertNotNull(result);
        assertEquals(walletId, result.getProgramWalletId());
        assertEquals(150, result.getGems());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnList() {
        UUID programId = UUID.randomUUID();

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setUserId(1L);
        dto.setGems(300L);

        when(programWalletService.getProgramWalletsByProgramId(programId))
                .thenReturn(List.of(dto));

        List<ProgramWalletBalanceDTO> result =
                programWalletController
                        .getProgramWalletsByProgramId(programId)
                        .getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(300, result.get(0).getGems());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void getProgramWalletsByProgramId_whenNoWallets_shouldReturnEmptyList() {
        UUID programId = UUID.randomUUID();

        when(programWalletService.getProgramWalletsByProgramId(programId))
                .thenReturn(List.of());

        List<ProgramWalletBalanceDTO> result =
                programWalletController
                        .getProgramWalletsByProgramId(programId)
                        .getBody();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnMultipleWallets() {
        UUID programId = UUID.randomUUID();

        ProgramWalletBalanceDTO dto1 = new ProgramWalletBalanceDTO();
        dto1.setProgramWalletId(UUID.randomUUID());
        dto1.setUserId(1L);
        dto1.setGems(100L);

        ProgramWalletBalanceDTO dto2 = new ProgramWalletBalanceDTO();
        dto2.setProgramWalletId(UUID.randomUUID());
        dto2.setUserId(2L);
        dto2.setGems(200L);

        when(programWalletService.getProgramWalletsByProgramId(programId))
                .thenReturn(List.of(dto1, dto2));

        List<ProgramWalletBalanceDTO> result =
                programWalletController
                        .getProgramWalletsByProgramId(programId)
                        .getBody();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100, result.get(0).getGems());
        assertEquals(200, result.get(1).getGems());
    }

    @Test
    void getMyProgramWallet_shouldReturnWallet() {
        UUID programId = UUID.randomUUID();

        ProgramWalletBalanceDTO dto = new ProgramWalletBalanceDTO();
        dto.setProgramWalletId(UUID.randomUUID());
        dto.setProgramId(programId);
        dto.setUserId(1L);
        dto.setGems(50L);

        when(programWalletService.getMyProgramWallet(programId))
                .thenReturn(dto);

        ProgramWalletBalanceDTO result =
                programWalletController
                        .getMyProgramWallet(programId)
                        .getBody();

        assertNotNull(result);
        assertEquals(programId, result.getProgramId());
        assertEquals(1L, result.getUserId());
        assertEquals(50, result.getGems());
    }

    @Test
    void getMyProgramWallet_whenWalletNotFound_shouldThrowException() {
        UUID programId = UUID.randomUUID();

        when(programWalletService.getMyProgramWallet(programId))
                .thenThrow(new RuntimeException("Program wallet not found"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programWalletController.getMyProgramWallet(programId)
        );

        assertEquals("Program wallet not found", ex.getMessage());
    }
}
