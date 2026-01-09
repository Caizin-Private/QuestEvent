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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @InjectMocks
    private ProgramWalletService programWalletService;

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

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programWalletService.createWallet(userId, programId)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createWallet_shouldFail_whenProgramNotFound() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> programWalletService.createWallet(userId, programId)
        );

        assertEquals("Program not found", ex.getMessage());
    }

    @Test
    void createWallet_shouldFail_whenWalletAlreadyExists() {

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
                .thenReturn(Optional.of(new ProgramWallet()));

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> programWalletService.createWallet(userId, programId)
        );

        assertEquals("Program wallet already exists", ex.getMessage());
        verify(programWalletRepository, never()).save(any());
    }

    @Test
    void getWalletBalanceByWalletId_shouldReturnBalance() {

        UUID walletId = UUID.randomUUID();
        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(walletId);
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(50L);

        when(programWalletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getWalletBalanceByWalletId(walletId);

        assertNotNull(dto);
        assertEquals(walletId, dto.getProgramWalletId());
        assertEquals(programId, dto.getProgramId());
        assertEquals(userId, dto.getUserId());
        assertEquals(50, dto.getGems());
    }

    @Test
    void getWalletBalanceByWalletId_shouldThrowNotFound_whenMissing() {

        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> programWalletService.getWalletBalanceByWalletId(UUID.randomUUID())
        );

        assertEquals("Program wallet not found", ex.getMessage());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnWallets() {

        UUID programId = UUID.randomUUID();
        Long userId = 1L;

        Program program = new Program();
        program.setProgramId(programId);

        User user = new User();
        user.setUserId(userId);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setProgram(program);
        wallet.setUser(user);
        wallet.setGems(300L);

        when(programWalletRepository.findByProgramProgramId(programId))
                .thenReturn(List.of(wallet));

        List<ProgramWalletBalanceDTO> result =
                programWalletService.getProgramWalletsByProgramId(programId);

        assertEquals(1, result.size());
        assertEquals(programId, result.get(0).getProgramId());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(300, result.get(0).getGems());
    }

    @Test
    void getProgramWalletsByProgramId_shouldThrowNotFound_whenEmpty() {

        UUID programId = UUID.randomUUID();

        when(programWalletRepository.findByProgramProgramId(programId))
                .thenReturn(List.of());

        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> programWalletService.getProgramWalletsByProgramId(programId)
        );

        assertEquals("No wallets found for this program", ex.getMessage());
    }

    @Test
    void getMyProgramWallet_shouldReturnWallet() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        UserPrincipal principal =
                new UserPrincipal(userId, "test@mail.com", null);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(80L);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(userId, programId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getMyProgramWallet(programId);

        assertEquals(userId, dto.getUserId());
        assertEquals(programId, dto.getProgramId());
        assertEquals(80, dto.getGems());

        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgramWallet_shouldThrowNotFound_whenUserMissing() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        UserPrincipal principal =
                new UserPrincipal(userId, "test@mail.com", null);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programWalletService.getMyProgramWallet(programId)
        );

        assertEquals("User not found", ex.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgramWallet_shouldThrowNotFound_whenWalletMissing() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        UserPrincipal principal =
                new UserPrincipal(userId, "test@mail.com", null);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(userId, programId))
                .thenReturn(Optional.empty());

        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> programWalletService.getMyProgramWallet(programId)
        );

        assertEquals("Program wallet not found", ex.getMessage());

        SecurityContextHolder.clearContext();
    }
}
