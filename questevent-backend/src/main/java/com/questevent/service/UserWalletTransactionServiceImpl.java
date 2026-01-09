package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserWalletTransactionServiceImpl implements UserWalletTransactionService {

    private final UserWalletRepository userWalletRepository;

    public UserWalletTransactionServiceImpl(UserWalletRepository userWalletRepository) {
        this.userWalletRepository = userWalletRepository;
    }

    @Override
    @Transactional
    public void creditGems(User user, int amount) {

        log.debug("Credit gems requested | user={} | amount={}", user, amount);

        if (user == null || user.getUserId() == null) {
            log.warn("Invalid user provided for gem credit");
            throw new IllegalArgumentException("Invalid user");
        }

        if (amount <= 0) {
            log.warn(
                    "Invalid credit amount={} for userId={}",
                    amount,
                    user.getUserId()
            );
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        UserWallet wallet = userWalletRepository
                .findByUserUserId(user.getUserId())
                .orElseThrow(() -> {
                    log.error("Wallet not found for userId={}", user.getUserId());
                    return new IllegalStateException("Wallet not found");
                });

        int before = wallet.getGems();
        wallet.setGems(before + amount);

        userWalletRepository.save(wallet);

        log.info(
                "Gems credited successfully | userId={} | walletId={} | before={} | credited={} | after={}",
                user.getUserId(),
                wallet.getWalletId(),
                before,
                amount,
                wallet.getGems()
        );
    }
}
