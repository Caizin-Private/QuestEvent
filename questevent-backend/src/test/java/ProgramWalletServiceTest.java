import com.questevent.dto.ProgramWalletBalanceDto;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
import com.questevent.service.ProgramWalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProgramWalletServiceTest {

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

        ProgramWallet wallet =
                programWalletService.createWallet(1L, 10L);

        assertNotNull(wallet);
        assertEquals(0, wallet.getGems());
        assertEquals(user, wallet.getUser());
        assertEquals(program, wallet.getProgram());
    }

    @Test
    void createWallet_shouldFail_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createWallet_shouldFail_whenProgramNotFound() {
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals("Program not found", ex.getMessage());
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

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programWalletService.createWallet(1L, 10L)
        );

        assertEquals("ProgramWallet already exists", ex.getMessage());
        verify(programWalletRepository, never()).save(any());
    }

    @Test
    void getUserProgramWalletBalances_shouldReturnAllProgramWalletBalances() {
        User user = new User();
        user.setUserId(1L);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(UUID.randomUUID());
        wallet.setGems(100);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programWalletRepository.findByUser(user))
                .thenReturn(List.of(wallet));

        List<ProgramWalletBalanceDto> balances =
                programWalletService.getUserProgramWalletBalances(1L);

        assertEquals(1, balances.size());
        assertEquals(100, balances.get(0).getGems());
    }

    @Test
    void getUserProgramWalletBalances_shouldThrowUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getUserProgramWalletBalances(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getUserProgramWalletBalances_shouldThrowNotFound_whenUserHasNoProgramWallets() {
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programWalletRepository.findByUser(user))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getUserProgramWalletBalances(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getWalletBalanceByWalletId_shouldReturnBalance() {
        UUID walletId = UUID.randomUUID();

        ProgramWallet wallet = new ProgramWallet();
        wallet.setProgramWalletId(walletId);
        wallet.setGems(50);

        when(programWalletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        ProgramWalletBalanceDto dto =
                programWalletService.getWalletBalanceByWalletId(walletId);

        assertEquals(walletId, dto.getProgramWalletId());
        assertEquals(50, dto.getGems());
    }

    @Test
    void getWalletBalanceByWalletId_shouldFail_whenWalletNotFound() {
        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programWalletService.getWalletBalanceByWalletId(
                        UUID.randomUUID()
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
