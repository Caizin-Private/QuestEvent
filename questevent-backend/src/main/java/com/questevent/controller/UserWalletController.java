package com.questevent.controller;

import com.questevent.dto.UserWalletBalanceDto;
import com.questevent.service.UserWalletService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/wallet")
public class UserWalletController {

    private final UserWalletService userWalletService;

    public UserWalletController(UserWalletService userWalletService) {
        this.userWalletService = userWalletService;
    }

    @GetMapping
    public UserWalletBalanceDto getWalletBalance(@PathVariable Long userId) {
        return userWalletService.getWalletBalance(userId);
    }
}
