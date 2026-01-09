package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Role;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.exception.WalletNotFoundException;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createWalletForUser_success() {

        Long userId = 1L;

        User user = new User();
        user.setUserId(userId);

        when(userWalletRepository.findByUserUserId(userId))
                .thenReturn(Optional.empty());

        userWalletService.createWalletForUser(user);

        verify(userWalletRepository, times(1))
                .save(any(UserWallet.class));
    }

    @Test
    void createWalletForUser_walletAlreadyExists() {

        Long userId = 1L;

        User user = new User();
        user.setUserId(userId);

        when(userWalletRepository.findByUserUserId(userId))
                .thenReturn(Optional.of(new UserWallet()));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> userWalletService.createWalletForUser(user)
        );

        assertEquals("Wallet already exists for user", exception.getMessage());
        verify(userWalletRepository, never()).save(any());
    }

    @Test
    void getMyWalletBalance_success() {

        Long userId = 1L;

        UserPrincipal principal =
                new UserPrincipal(userId, "test@example.com", Role.USER);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(userId);

        UUID walletId = UUID.randomUUID();
        UserWallet wallet = new UserWallet();
        wallet.setWalletId(walletId);
        wallet.setGems(100L);

        user.setWallet(wallet);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserWalletBalanceDTO dto =
                userWalletService.getMyWalletBalance();

        assertNotNull(dto);
        assertEquals(walletId, dto.getWalletId());
        assertEquals(100L, dto.getGems());
    }

    @Test
    void getMyWalletBalance_userNotFound() {

        Long userId = 1L;

        UserPrincipal principal =
                new UserPrincipal(userId, "test@example.com", Role.USER);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getMyWalletBalance_walletNotFound() {

        Long userId = 1L;

        UserPrincipal principal =
                new UserPrincipal(userId, "test@example.com", Role.USER);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setUserId(userId);
        user.setWallet(null);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        WalletNotFoundException exception = assertThrows(
                WalletNotFoundException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("Wallet not found", exception.getMessage());
    }

    @Test
    void getMyWalletBalance_unauthenticated() {

        SecurityContextHolder.clearContext();

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("Unauthenticated request", exception.getMessage());
    }
}
