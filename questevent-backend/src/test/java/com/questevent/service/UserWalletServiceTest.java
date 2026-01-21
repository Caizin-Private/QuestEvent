package com.questevent.service;

import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.exception.WalletNotFoundException;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserWalletServiceTest {

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserWalletService userWalletService;

    private User user;
    private UserWallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");

        wallet = new UserWallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setGems(50L);
        wallet.setUser(user);
        wallet.setCreatedAt(Instant.now());
        wallet.setUpdatedAt(Instant.now());

        user.setWallet(wallet);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ----------------------------------------------------------------
    // createWalletForUser
    // ----------------------------------------------------------------

    @Test
    void createWalletForUser_success() {
        when(userWalletRepository.findByUserUserId(user.getUserId()))
                .thenReturn(Optional.empty());

        userWalletService.createWalletForUser(user);

        verify(userWalletRepository).save(any(UserWallet.class));
    }

    @Test
    void createWalletForUser_nullUser_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userWalletService.createWalletForUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid user");
    }

    @Test
    void createWalletForUser_nullUserId_throwsIllegalArgumentException() {
        user.setUserId(null);

        assertThatThrownBy(() -> userWalletService.createWalletForUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid user");
    }

    @Test
    void createWalletForUser_walletAlreadyExists_throwsResourceConflictException() {
        when(userWalletRepository.findByUserUserId(user.getUserId()))
                .thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> userWalletService.createWalletForUser(user))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Wallet already exists for user");

        verify(userWalletRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // getMyWalletBalance
    // ----------------------------------------------------------------

    @Test
    void getMyWalletBalance_success() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", user.getEmail())
                .build();

        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserWalletBalanceDTO dto = userWalletService.getMyWalletBalance();

        assertThat(dto.getWalletId()).isEqualTo(wallet.getWalletId());
        assertThat(dto.getGems()).isEqualTo(wallet.getGems());
        assertThat(dto.getCreatedAt()).isEqualTo(wallet.getCreatedAt());
        assertThat(dto.getUpdatedAt()).isEqualTo(wallet.getUpdatedAt());
    }

    @Test
    void getMyWalletBalance_invalidAuthentication_throwsUnauthorizedException() {
        Authentication auth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> userWalletService.getMyWalletBalance())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid authentication");
    }

    @Test
    void getMyWalletBalance_missingEmailClaim_throwsUnauthorizedException() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "123456") // ✅ required non-empty claim
                .build();

        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> userWalletService.getMyWalletBalance())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid token");
    }


    @Test
    void getMyWalletBalance_userNotFound_throwsUserNotFoundException() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", user.getEmail())
                .build();

        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userWalletService.getMyWalletBalance())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getMyWalletBalance_walletNotFound_throwsWalletNotFoundException() {
        user.setWallet(null);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", user.getEmail())
                .build();

        Authentication auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userWalletService.getMyWalletBalance())
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Wallet not found");
    }
}
