package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWalletTransactionServiceImpl implements UserWalletTransactionService {

    private final UserWalletRepository userWalletRepository;

    public UserWalletTransactionServiceImpl(UserWalletRepository userWalletRepository) {
        this.userWalletRepository = userWalletRepository;
    }

    @Override
    @Transactional
    public void creditGems(User user, int amount) {

        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        UserWallet wallet = userWalletRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() ->
                        new IllegalStateException("Wallet not found")
                );

        wallet.setGems(wallet.getGems() + amount);
        userWalletRepository.save(wallet);
    }
}
