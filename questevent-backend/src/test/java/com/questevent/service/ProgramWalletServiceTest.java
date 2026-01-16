package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.exception.WalletNotFoundException;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramWalletServiceTest {

    @Mock
    private ProgramWalletRepository programWalletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private SecurityUserResolver securityUserResolver; // ✅ REQUIRED

    @InjectMocks
    private ProgramWalletService programWalletService;

    /* ================= CREATE WALLET ================= */

    @Test
    void createWallet_shouldCreateWallet() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.empty());
        when(programWalletRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgramWallet wallet =
                programWalletService.createWallet(userId, programId);

        assertNotNull(wallet);
        assertEquals(0, wallet.getGems());
        assertEquals(user, wallet.getUser());
        assertEquals(program, wallet.getProgram());
    }

    @Test
    void createWallet_shouldFail_whenUserNotFound() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programWalletService.createWallet(1L, UUID.randomUUID())
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createWallet_shouldFail_whenProgramNotFound() {

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(any()))
                .thenReturn(Optional.empty());

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> programWalletService.createWallet(1L, UUID.randomUUID())
        );

        assertEquals("Program not found", ex.getMessage());
    }

    @Test
    void createWallet_shouldFail_whenWalletAlreadyExists() {

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(any()))
                .thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.of(new ProgramWallet()));

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> programWalletService.createWallet(1L, UUID.randomUUID())
        );

        assertEquals("Program wallet already exists", ex.getMessage());
        verify(programWalletRepository, never()).save(any());
    }

    /* ================= FETCH ================= */

    @Test
    void getWalletBalanceByWalletId_shouldReturnBalance() {

        UUID walletId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(UUID.randomUUID());

        User user = new User();
        user.setUserId(1L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(walletId);
        wallet.setProgram(program);
        wallet.setUser(user);
        wallet.setGems(50L);

        when(programWalletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getWalletBalanceByWalletId(walletId);

        assertEquals(50, dto.getGems());
        assertEquals(user.getUserId(), dto.getUserId());
        assertEquals(program.getProgramId(), dto.getProgramId());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnWallets() {

        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        User user = new User();
        user.setUserId(1L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgram(program);
        wallet.setUser(user);
        wallet.setGems(300L);

        when(programWalletRepository.findByProgramProgramId(programId))
                .thenReturn(List.of(wallet));

        List<ProgramWalletBalanceDTO> result =
                programWalletService.getProgramWalletsByProgramId(programId);

        assertEquals(1, result.size());
        assertEquals(300, result.get(0).getGems());
    }

    /* ================= MY WALLET ================= */

    @Test
    void getMyProgramWallet_shouldReturnWallet() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        when(securityUserResolver.getCurrentUserPrincipal())
                .thenReturn(new UserPrincipal(userId, "test@mail.com", null));

        Program program = new Program();
        program.setProgramId(programId);

        User user = new User();
        user.setUserId(userId);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgram(program);
        wallet.setUser(user);
        wallet.setGems(80L);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(userId, programId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getMyProgramWallet(programId);

        assertEquals(80, dto.getGems());
        assertEquals(userId, dto.getUserId());
    }

    @Test
    void getMyProgramWallet_shouldThrow_whenWalletMissing() {

        when(securityUserResolver.getCurrentUserPrincipal())
                .thenReturn(new UserPrincipal(1L, "test@mail.com", null));

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(any(), any()))
                .thenReturn(Optional.empty());

        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> programWalletService.getMyProgramWallet(UUID.randomUUID())
        );

        assertEquals("Program wallet not found", ex.getMessage());
    }
}
