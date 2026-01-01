package com.questevent.service;

import com.questevent.dto.UserWalletBalanceDto;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserWalletServiceTest {

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserWalletService userWalletService;
    @Test
    void createWalletForUser_success() {
        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        userWalletService.createWalletForUser(user);

        verify(userWalletRepository, times(1)).save(any(UserWallet.class));
    }
    @Test
    void createWalletForUser_walletAlreadyExists() {
        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(new UserWallet()));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userWalletService.createWalletForUser(user)
        );

        assertEquals("Wallet already exists for user", exception.getMessage());
        verify(userWalletRepository, never()).save(any());
    }

    @Test
    void getWalletBalance_success() {
        User user = new User();
        user.setUserId(1L);

        UserWallet wallet = new UserWallet();
        UUID walletId = UUID.randomUUID();
        wallet.setWalletId(walletId);
        wallet.setGems(100);

        user.setWallet(wallet);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserWalletBalanceDto dto = userWalletService.getWalletBalance(1L);

        assertNotNull(dto);
        assertEquals(walletId, dto.getWalletId());
        assertEquals(100, dto.getGems());
    }


    @Test
    void getWalletBalance_userNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userWalletService.getWalletBalance(1L)
        );

        assertEquals("404 NOT_FOUND \"User not found\"", exception.getMessage());
    }

    @Test
    void getWalletBalance_walletNotFound() {
        User user = new User();
        user.setUserId(1L);
        user.setWallet(null);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userWalletService.getWalletBalance(1L)
        );

        assertEquals("404 NOT_FOUND \"Wallet not found\"", exception.getMessage());
    }
}
