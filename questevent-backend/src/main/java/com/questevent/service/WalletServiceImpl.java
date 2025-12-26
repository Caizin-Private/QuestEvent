package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.Wallet;
import com.questevent.repository.WalletRepository;

public class WalletServiceImpl implements WalletService {

    public static final int INITIAL_COINS = 500;

    private final WalletRepository walletRepository;

    public WalletServiceImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet createWalletForUser(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCoins(INITIAL_COINS);
        wallet.setGems(0);

        return walletRepository.save(wallet);
    }

    @Override
    public void deductCoins(User user, int amount) {
        Wallet wallet = walletRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getCoins() < amount) {
            throw new RuntimeException("Insufficient coins");
        }

        wallet.setCoins(wallet.getCoins() - amount);
        walletRepository.save(wallet);
    }

    @Override
    public void addGems(User user, int gems) {
        Wallet wallet = walletRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setGems(wallet.getGems() + gems);
        walletRepository.save(wallet);
    }
}
