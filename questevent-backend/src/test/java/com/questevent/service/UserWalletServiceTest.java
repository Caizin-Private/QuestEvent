package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Role;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createWalletForUser_success() {
        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.empty());

        userWalletService.createWalletForUser(user);

        verify(userWalletRepository)
                .save(any(UserWallet.class));
    }

    @Test
    void createWalletForUser_walletAlreadyExists() {
        User user = new User();
        user.setUserId(1L);

        when(userWalletRepository.findByUserUserId(1L))
                .thenReturn(Optional.of(new UserWallet()));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> userWalletService.createWalletForUser(user)
        );

        assertEquals(
                "Wallet already exists for user",
                ex.getMessage()
        );

        verify(userWalletRepository, never()).save(any());
    }

    @Test
    void getMyWalletBalance_success() {
        UserPrincipal principal =
                new UserPrincipal(1L, "test@example.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null)
        );

        User user = new User();
        user.setUserId(1L);

        UUID walletId = UUID.randomUUID();
        UserWallet wallet = new UserWallet();
        wallet.setWalletId(walletId);
        wallet.setGems(100);

        user.setWallet(wallet);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserWalletBalanceDTO dto =
                userWalletService.getMyWalletBalance();

        assertNotNull(dto);
        assertEquals(walletId, dto.getWalletId());
        assertEquals(100, dto.getGems());
    }

    @Test
    void getMyWalletBalance_userNotFound() {
        UserPrincipal principal =
                new UserPrincipal(1L, "test@example.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null)
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("User not found", ex.getReason());
    }

    @Test
    void getMyWalletBalance_walletNotFound() {
        UserPrincipal principal =
                new UserPrincipal(1L, "test@example.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null)
        );

        User user = new User();
        user.setUserId(1L);
        user.setWallet(null);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("Wallet not found", ex.getReason());
    }

    @Test
    void getMyWalletBalance_unauthenticated() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userWalletService.getMyWalletBalance()
        );

        assertEquals("Unauthenticated request", ex.getReason());
    }
}
