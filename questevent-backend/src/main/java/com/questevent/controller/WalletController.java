package com.questevent.controller;

import com.questevent.dto.WalletBalanceDto;
import com.questevent.service.WalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletBalanceDto getWalletBalance(@PathVariable Long userId) {
        return walletService.getWalletBalance(userId);
    }
}
