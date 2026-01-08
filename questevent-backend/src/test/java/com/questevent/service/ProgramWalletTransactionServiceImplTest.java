package com.questevent.service;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProgramWalletTransactionServiceImplTest {

    @Mock
    private ProgramWalletRepository programWalletRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @InjectMocks
    private ProgramWalletTransactionServiceImpl service;

    @Test
    void creditGems_shouldIncreaseBalance_whenValidInput() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(100L);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.of(wallet));

        service.creditGems(user, program, 50L);

        assertEquals(150, wallet.getGems());
        verify(programWalletRepository).save(wallet);
    }

    @Test
    void creditGems_shouldThrowException_whenAmountIsZero() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.creditGems(user, program, 0L)
        );

        assertEquals("Amount must be greater than zero", ex.getMessage());
        verifyNoInteractions(programWalletRepository);
    }

    @Test
    void creditGems_shouldThrowException_whenAmountIsNegative() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.creditGems(user, program, (long) -20)
        );

        verifyNoInteractions(programWalletRepository);
    }

    @Test
    void creditGems_shouldThrowException_whenWalletNotFound() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.creditGems(user, program, 10L)
        );

        assertEquals("Program wallet not found", ex.getMessage());
        verify(programWalletRepository, never()).save(any());
    }

    @Test
    void creditGems_shouldHandleLargeAmount() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(1_000_000L);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.of(wallet));

        service.creditGems(user, program, 500_000L);

        assertEquals(1_500_000, wallet.getGems());
        verify(programWalletRepository).save(wallet);
    }

    @Test
    void autoSettleExpiredProgramWallets_shouldTransferGemsAndCompleteProgram() {
        Program program = new Program();
        program.setProgramId(10L);
        program.setStatus(ProgramStatus.ACTIVE);

        User user = new User();
        user.setUserId(1L);

        UserWallet userWallet = new UserWallet();
        userWallet.setGems(100L);

        user.setWallet(userWallet);

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setGems(50L);

        when(programRepository
                .findByStatusAndEndDateBefore(eq(ProgramStatus.ACTIVE), any()))
                .thenReturn(List.of(program));

        when(programWalletRepository
                .findByProgramProgramId(10L))
                .thenReturn(List.of(programWallet));

        when(userWalletRepository
                .findByUserUserId(1L))
                .thenReturn(Optional.of(userWallet));

        service.autoSettleExpiredProgramWallets();

        assertEquals(150, userWallet.getGems());
        assertEquals(0, programWallet.getGems());
        assertEquals(ProgramStatus.COMPLETED, program.getStatus());

        verify(userWalletRepository).save(userWallet);
        verify(programWalletRepository).save(programWallet);
        verify(programRepository).save(program);
    }

    @Test
    void autoSettleExpiredProgramWallets_shouldSkipWalletsWithZeroGems() {
        Program program = new Program();
        program.setProgramId(10L);
        program.setStatus(ProgramStatus.ACTIVE);

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setGems(0L);

        when(programRepository
                .findByStatusAndEndDateBefore(eq(ProgramStatus.ACTIVE), any()))
                .thenReturn(List.of(program));

        when(programWalletRepository
                .findByProgramProgramId(10L))
                .thenReturn(List.of(programWallet));

        service.autoSettleExpiredProgramWallets();

        verifyNoInteractions(userWalletRepository);
        verify(programRepository).save(program);
    }

    @Test
    void manuallySettleExpiredProgramWallets_shouldSettleAndCompleteProgram() {
        Program program = new Program();
        program.setProgramId(10L);
        program.setStatus(ProgramStatus.ACTIVE);

        UserWallet userWallet = new UserWallet();
        userWallet.setGems(200L);

        User user = new User();
        user.setWallet(userWallet);

        ProgramWallet programWallet = new ProgramWallet();
        programWallet.setUser(user);
        programWallet.setGems(50L);

        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        when(programWalletRepository
                .findByProgramProgramId(10L))
                .thenReturn(List.of(programWallet));

        service.manuallySettleExpiredProgramWallets(10L);

        assertEquals(250, userWallet.getGems());
        assertEquals(0, programWallet.getGems());
        assertEquals(ProgramStatus.COMPLETED, program.getStatus());

        verify(userWalletRepository).save(userWallet);
        verify(programWalletRepository).save(programWallet);
        verify(programRepository).save(program);
    }

    @Test
    void manuallySettleExpiredProgramWallets_shouldThrowException_whenProgramCompleted() {
        Program program = new Program();
        program.setStatus(ProgramStatus.COMPLETED);

        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.manuallySettleExpiredProgramWallets(10L)
        );

        assertEquals("Program already completed", ex.getMessage());
    }

    @Test
    void manuallySettleExpiredProgramWallets_shouldThrowException_whenProgramIdInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.manuallySettleExpiredProgramWallets(0L)
        );
    }
}
