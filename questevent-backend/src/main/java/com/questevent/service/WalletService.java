package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.Wallet;

public interface WalletService {
    Wallet createWalletForUser(User user);

    void deductCoins(User user, int amount);

    void addGems(User user, int gems);
}
