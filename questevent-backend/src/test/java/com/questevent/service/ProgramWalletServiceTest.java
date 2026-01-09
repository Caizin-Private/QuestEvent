package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

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
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.empty());
        when(programWalletRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgramWallet wallet = programWalletService.createWallet(1L, 10L);

        assertNotNull(wallet);
        assertEquals(0, wallet.getGems());
        assertEquals(user, wallet.getUser());
        assertEquals(program, wallet.getProgram());
    }

    @Test
    void createWallet_shouldFail_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    @Test
    void createWallet_shouldFail_whenProgramNotFound() {
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void createWallet_shouldFail_whenWalletAlreadyExists() {
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.of(new ProgramWallet()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Program wallet already exists", ex.getReason());
        verify(programWalletRepository, never()).save(any());
    }

    @Test
    void getWalletBalanceByWalletId_shouldReturnBalance() {
        UUID walletId = UUID.randomUUID();

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(walletId);
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(50);

        when(programWalletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getWalletBalanceByWalletId(walletId);

        assertEquals(walletId, dto.getProgramWalletId());
        assertEquals(10L, dto.getProgramId());
        assertEquals(1L, dto.getUserId());
        assertEquals(50, dto.getGems());
    }

    @Test
    void getWalletBalanceByWalletId_shouldThrowNotFound() {
        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getWalletBalanceByWalletId(UUID.randomUUID())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program wallet not found", ex.getReason());
    }

    @Test
    void getProgramWalletsByProgramId_shouldReturnWallets() {
        Program program = new Program();
        program.setProgramId(10L);

        User user = new User();
        user.setUserId(1L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setProgram(program);
        wallet.setUser(user);
        wallet.setGems(300);

        when(programWalletRepository.findByProgramProgramId(10L))
                .thenReturn(List.of(wallet));

        List<ProgramWalletBalanceDTO> result =
                programWalletService.getProgramWalletsByProgramId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getProgramId());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(300, result.get(0).getGems());
    }

    @Test
    void getProgramWalletsByProgramId_shouldThrowNotFound_whenEmpty() {
        when(programWalletRepository.findByProgramProgramId(10L))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getProgramWalletsByProgramId(10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("No wallets found for this program", ex.getReason());
    }

    @Test
    void getMyProgramWallet_shouldReturnWallet() {
        UserPrincipal principal = new UserPrincipal(1L, "test@mail.com", null);
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(10L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(80);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                programWalletService.getMyProgramWallet(10L);

        assertEquals(1L, dto.getUserId());
        assertEquals(10L, dto.getProgramId());
        assertEquals(80, dto.getGems());

        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgramWallet_shouldThrowUserNotFound() {
        UserPrincipal principal = new UserPrincipal(1L, "test@mail.com", null);
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getMyProgramWallet(10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());

        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProgramWallet_shouldThrowWalletNotFound() {
        UserPrincipal principal = new UserPrincipal(1L, "test@mail.com", null);
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getMyProgramWallet(10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program wallet not found", ex.getReason());

        SecurityContextHolder.clearContext();
    }
}
