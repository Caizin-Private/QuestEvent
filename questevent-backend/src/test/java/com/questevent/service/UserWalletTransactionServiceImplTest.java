package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserWalletRepository;
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

    private void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void creditGems_success() {
        initMocks();

        User user = new User();
        user.setUserId(1L);

        UserWallet wallet = new UserWallet();
        wallet.setGems(100);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(wallet));

        userWalletTransactionService.creditGems(user, 50);

        assertEquals(150, wallet.getGems());
        verify(userWalletRepository).save(wallet);
    }

    @Test
    void creditGems_amountZero_shouldThrowException() {
        initMocks();

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
        initMocks();

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
        initMocks();

        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> userWalletTransactionService.creditGems(user, 20)
        );

        assertEquals("Wallet not found", ex.getMessage());
        verify(userWalletRepository, never()).save(any());
    }
}
