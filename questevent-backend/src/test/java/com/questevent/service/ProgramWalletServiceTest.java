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
import org.junit.jupiter.api.function.Executable;
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
    void getWalletBalanceByWalletId_shouldThrowNotFound_whenMissing() {

        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        Executable executable =
                () -> programWalletService.getWalletBalanceByWalletId(UUID.randomUUID());

        WalletNotFoundException ex =
                assertThrows(WalletNotFoundException.class, executable);

        assertEquals("Program wallet not found", ex.getMessage());
    }

    @Test
    void getProgramWalletsByProgramId_shouldThrowNotFound_whenEmpty() {

        UUID programId = UUID.randomUUID();

        when(programWalletRepository.findByProgramProgramId(programId))
                .thenReturn(List.of());

        Executable executable =
                () -> programWalletService.getProgramWalletsByProgramId(programId);

        WalletNotFoundException ex =
                assertThrows(WalletNotFoundException.class, executable);

        assertEquals("No wallets found for this program", ex.getMessage());
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

        Executable executable =
                () -> programWalletService.getMyProgramWallet(programId);

        UserNotFoundException ex =
                assertThrows(UserNotFoundException.class, executable);

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

        Executable executable =
                () -> programWalletService.getMyProgramWallet(programId);

        WalletNotFoundException ex =
                assertThrows(WalletNotFoundException.class, executable);

        assertEquals("Program wallet not found", ex.getMessage());

        SecurityContextHolder.clearContext();
    }
}
