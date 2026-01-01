package com.questevent.service;

import com.questevent.dto.UserWalletBalanceDto;
import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;

    public UserWalletService(UserWalletRepository userWalletRepository, UserRepository userRepository) {
        this.userWalletRepository = userWalletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createWalletForUser(User user) {

        userWalletRepository.findByUserUserId(user.getUserId())
                .ifPresent(w -> {
                    throw new RuntimeException("Wallet already exists for user");
                });

        UserWallet userWallet = new UserWallet();
        userWallet.setUser(user);
        userWallet.setGems(0);

        userWalletRepository.save(userWallet);
    }

    public UserWalletBalanceDto getWalletBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        UserWallet wallet = user.getWallet();
        if (wallet == null) {
            throw new ResponseStatusException(NOT_FOUND, "Wallet not found");
        }

        UserWalletBalanceDto dto = new UserWalletBalanceDto();
        dto.setWalletId(wallet.getWalletId());
        dto.setGems(wallet.getGems());

        return dto;
    }
}

