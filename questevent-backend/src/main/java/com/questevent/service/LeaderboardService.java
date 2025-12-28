package com.questevent.service;

import com.questevent.dto.LeaderboardDto;
import com.questevent.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    private final WalletRepository walletRepository;

    public LeaderboardService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public List<LeaderboardDto> getGlobalLeaderboard() {
        return walletRepository.findGlobalLeaderboard()
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
