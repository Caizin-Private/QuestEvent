package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.Wallet;
import com.questevent.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private static final int INITIAL_COINS = 500;

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public void createWalletForUser(User user) {

        walletRepository.findByUserUserId(user.getUserId())
                .ifPresent(w -> {
                    throw new RuntimeException("Wallet already exists for user");
                });

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCoins(INITIAL_COINS);
        wallet.setGems(0);

        walletRepository.save(wallet);
    }
}

