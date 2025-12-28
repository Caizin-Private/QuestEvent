package com.questevent.service;

import com.questevent.dto.WalletBalanceDto;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserRepository;
import com.questevent.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository , UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createWalletForUser(User user) {

        walletRepository.findByUserUserId(user.getUserId())
                .ifPresent(w -> {
                    throw new RuntimeException("Wallet already exists for user");
                });

        UserWallet userWallet = new UserWallet();
        userWallet.setUser(user);
        userWallet.setGems(0);

        walletRepository.save(userWallet);
    }

    public WalletBalanceDto getWalletBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            throw new ResponseStatusException(NOT_FOUND, "Wallet not found");
        }

        WalletBalanceDto dto = new WalletBalanceDto();
        dto.setWalletId(wallet.getWalletId());
        dto.setGems(wallet.getGems());

        return dto;
    }
}

