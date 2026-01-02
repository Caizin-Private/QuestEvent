package com.questevent.service;

import com.questevent.dto.LeaderboardDTO;
import com.questevent.repository.UserWalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private final UserWalletRepository userWalletRepository;

    public LeaderboardService(UserWalletRepository userWalletRepository) {
        this.userWalletRepository = userWalletRepository;
    }

    public List<LeaderboardDTO> getGlobalLeaderboard() {
        return userWalletRepository.findGlobalLeaderboard()
                .stream()
                .map(wallet -> {
                    LeaderboardDTO dto = new LeaderboardDTO();
                    dto.setUserId(wallet.getUser().getUserId());
                    dto.setUserName(wallet.getUser().getName());
                    dto.setGems(wallet.getGems());
                    return dto;
                })
                .toList();
    }
}
