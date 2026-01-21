package com.questevent.service;

import com.questevent.dto.ProgramWalletBalanceDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
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
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private ProgramWalletService service;

    private User user;
    private Program program;
    private ProgramWallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);

        program = new Program();
        program.setProgramId(UUID.randomUUID());

        wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setProgram(program);
        wallet.setGems(100L);
    }

    @Test
    void createWallet_success() {
        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.empty());
        when(programWalletRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ProgramWallet result =
                service.createWallet(user.getUserId(), program.getProgramId());

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getProgram()).isEqualTo(program);
        assertThat(result.getGems()).isZero();
    }

    @Test
    void createWallet_userNotFound() {
        when(userRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createWallet(1L, program.getProgramId()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createWallet_programNotFound() {
        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createWallet(user.getUserId(), UUID.randomUUID()))
                .isInstanceOf(ProgramNotFoundException.class);
    }

    @Test
    void createWallet_alreadyExists() {
        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(programWalletRepository.findByUserAndProgram(user, program))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() ->
                service.createWallet(user.getUserId(), program.getProgramId()))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void getWalletBalanceByWalletId_success() {
        when(programWalletRepository.findById(wallet.getProgramWalletId()))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                service.getWalletBalanceByWalletId(wallet.getProgramWalletId());

        assertThat(dto.getProgramWalletId()).isEqualTo(wallet.getProgramWalletId());
        assertThat(dto.getUserId()).isEqualTo(user.getUserId());
        assertThat(dto.getProgramId()).isEqualTo(program.getProgramId());
        assertThat(dto.getGems()).isEqualTo(100L);
    }

    @Test
    void getWalletBalanceByWalletId_notFound() {
        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getWalletBalanceByWalletId(UUID.randomUUID()))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getProgramWalletsByProgramId_success() {
        when(programWalletRepository.findByProgramProgramId(program.getProgramId()))
                .thenReturn(List.of(wallet));

        List<ProgramWalletBalanceDTO> result =
                service.getProgramWalletsByProgramId(program.getProgramId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGems()).isEqualTo(100L);
    }

    @Test
    void getProgramWalletsByProgramId_empty() {
        when(programWalletRepository.findByProgramProgramId(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.getProgramWalletsByProgramId(UUID.randomUUID()))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getMyProgramWallet_success() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(
                        user.getUserId(),
                        program.getProgramId()))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDTO dto =
                service.getMyProgramWallet(program.getProgramId());

        assertThat(dto.getUserId()).isEqualTo(user.getUserId());
        assertThat(dto.getProgramId()).isEqualTo(program.getProgramId());
        assertThat(dto.getGems()).isEqualTo(100L);
    }

    @Test
    void getMyProgramWallet_notFound() {
        when(securityUserResolver.getCurrentUser())
                .thenReturn(user);
        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getMyProgramWallet(program.getProgramId()))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
