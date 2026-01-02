import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.service.ProgramWalletTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramWalletTransactionServiceImplTest {

    @Mock
    private ProgramWalletRepository programWalletRepository;

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
        wallet.setGems(100);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.of(wallet));

        service.creditGems(user, program, 50);

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
                () -> service.creditGems(user, program, 0)
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
                () -> service.creditGems(user, program, -20)
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
                () -> service.creditGems(user, program, 10)
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
        wallet.setGems(1_000_000);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(1L, 10L))
                .thenReturn(Optional.of(wallet));

        service.creditGems(user, program, 500_000);

        assertEquals(1_500_000, wallet.getGems());
        verify(programWalletRepository).save(wallet);
    }
}
