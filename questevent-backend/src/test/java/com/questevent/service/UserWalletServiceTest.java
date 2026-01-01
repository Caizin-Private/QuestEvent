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
}
