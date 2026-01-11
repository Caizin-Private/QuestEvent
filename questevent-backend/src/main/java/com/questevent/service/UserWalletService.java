package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.exception.WalletNotFoundException;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;

    public UserWalletService(UserWalletRepository userWalletRepository,
                             UserRepository userRepository) {
        this.userWalletRepository = userWalletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createWalletForUser(User user) {

        log.debug("Attempting to create wallet for user: {}", user);

        if (user == null || user.getUserId() == null) {
            log.error("Invalid user provided while creating wallet");
            throw new IllegalArgumentException("Invalid user");
        }

        userWalletRepository.findByUserUserId(user.getUserId())
                .ifPresent(w -> {
                    log.error("Wallet already exists for userId={}", user.getUserId());
                    throw new ResourceConflictException("Wallet already exists for user");
                });

        UserWallet userWallet = new UserWallet();
        userWallet.setUser(user);
        userWallet.setGems(0L);

        userWalletRepository.save(userWallet);

        log.info("Wallet successfully created for userId={}", user.getUserId());
    }

    public UserWalletBalanceDTO getMyWalletBalance() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request to fetch wallet balance");
            throw new UnauthorizedException("Unauthenticated request");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal p)) {
            log.warn("Invalid authentication principal: {}", principal);
            throw new UnauthorizedException("Invalid authentication principal");
        }

        log.debug("Fetching wallet balance for userId={}", p.userId());

        User user = userRepository.findById(p.userId())
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", p.userId());
                    return new UserNotFoundException("User not found");
                });

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            log.warn("Wallet not found for userId={}", p.userId());
            throw new WalletNotFoundException("Wallet not found");
        }

        UserWalletBalanceDTO dto = new UserWalletBalanceDTO();
        dto.setWalletId(wallet.getWalletId());
        dto.setGems(wallet.getGems());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setUpdatedAt(wallet.getUpdatedAt());

        log.info(
                "Wallet balance fetched for userId={}, walletId={}, gems={}",
                p.userId(),
                wallet.getWalletId(),
                wallet.getGems()
        );

        return dto;
    }
}
