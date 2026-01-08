package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
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

        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        userWalletRepository.findByUserUserId(user.getUserId())
                .ifPresent(w -> {
                    throw new IllegalStateException(
                            "Wallet already exists for user"
                    );
                });

        UserWallet userWallet = new UserWallet();
        userWallet.setUser(user);
        userWallet.setGems(0L);

        userWalletRepository.save(userWallet);
    }

    public UserWalletBalanceDTO getMyWalletBalance() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Unauthenticated request"
            );
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal p)) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Invalid authentication principal"
            );
        }

        User user = userRepository.findById(p.userId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                NOT_FOUND,
                                "User not found"
                        )
                );

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Wallet not found"
            );
        }

        UserWalletBalanceDTO dto = new UserWalletBalanceDTO();
        dto.setWalletId(wallet.getWalletId());
        dto.setGems(wallet.getGems());

        return dto;
    }
}
