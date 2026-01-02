package com.questevent.service;

import com.questevent.dto.LeaderboardDto;
import com.questevent.repository.UserWalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private final UserWalletRepository userWalletRepository;

    public LeaderboardService(UserWalletRepository userWalletRepository) {
        this.userWalletRepository = userWalletRepository;
    }

    public List<LeaderboardDto> getGlobalLeaderboard() {
        return userWalletRepository.findGlobalLeaderboard()
                .stream()
                .map(wallet -> {
                    LeaderboardDto dto = new LeaderboardDto();
                    dto.setUserId(wallet.getUser().getUserId());
                    dto.setUserName(wallet.getUser().getName());
                    dto.setGems(wallet.getGems());
                    return dto;
                })
                .toList();
    }
}
