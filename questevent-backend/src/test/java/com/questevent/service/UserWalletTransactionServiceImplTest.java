package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserWalletTransactionServiceImplTest {

    @Mock
    private UserWalletRepository userWalletRepository;

    @InjectMocks
    private UserWalletTransactionServiceImpl userWalletTransactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void creditGems_success() {
        User user = new User();
        user.setUserId(1L);

        UserWallet wallet = new UserWallet();
        wallet.setGems(100);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(wallet));

        userWalletTransactionService.creditGems(user, 50);

        assertEquals(150, wallet.getGems());
        verify(userWalletRepository, times(1)).save(wallet);
    }

    @Test
    void creditGems_amountZero_shouldThrowException() {
        User user = new User();
        user.setUserId(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userWalletTransactionService.creditGems(user, 0)
        );

        assertEquals("Amount must be greater than zero", ex.getMessage());
        verifyNoInteractions(userWalletRepository);
    }

    @Test
    void creditGems_amountNegative_shouldThrowException() {
        User user = new User();
        user.setUserId(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userWalletTransactionService.creditGems(user, -10)
        );

        assertEquals("Amount must be greater than zero", ex.getMessage());
        verifyNoInteractions(userWalletRepository);
    }


    @Test
    void creditGems_walletNotFound_shouldThrowException() {
        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> userWalletTransactionService.creditGems(user, 20)
        );

        assertEquals("Wallet not found", ex.getMessage());
        verify(userWalletRepository, never()).save(any());
    }
}
