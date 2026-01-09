package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.exception.WalletNotFoundException;
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

        Long userId = 1L;

        User user = new User();
        user.setUserId(userId);

        UserWallet wallet = new UserWallet();
        wallet.setGems(100L);

        when(userWalletRepository.findByUserUserId(userId))
                .thenReturn(Optional.of(wallet));

        userWalletTransactionService.creditGems(user, 50L);

        assertEquals(150L, wallet.getGems());
        verify(userWalletRepository, times(1)).save(wallet);
    }

    @Test
    void creditGems_amountZero_shouldThrowException() {

        User user = new User();
        user.setUserId(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userWalletTransactionService.creditGems(user, 0L)
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
                () -> userWalletTransactionService.creditGems(user, -10L)
        );

        assertEquals("Amount must be greater than zero", ex.getMessage());
        verifyNoInteractions(userWalletRepository);
    }

    @Test
    void creditGems_walletNotFound_shouldThrowException() {

        Long userId = 1L;

        User user = new User();
        user.setUserId(userId);

        when(userWalletRepository.findByUserUserId(userId))
                .thenReturn(Optional.empty());

        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> userWalletTransactionService.creditGems(user, 20L)
        );

        assertEquals("Wallet not found", ex.getMessage());
        verify(userWalletRepository, never()).save(any());
    }
}
