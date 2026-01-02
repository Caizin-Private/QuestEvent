package com.questevent.controller;

import com.questevent.dto.WalletBalanceDto;
import com.questevent.service.WalletService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PreAuthorize("@rbac.canAccessUserWallet(authentication, #userId)")
    @GetMapping
    public WalletBalanceDto getWalletBalance(@PathVariable Long userId) {
        return walletService.getWalletBalance(userId);
    }
}
