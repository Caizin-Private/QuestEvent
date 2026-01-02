package com.questevent.controller;

import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.service.UserWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
@Tag(name = "User Wallets", description = "User wallet management APIs")
public class UserWalletController {

    private final UserWalletService walletService;

    public UserWalletController(UserWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @Operation(summary = "Get user wallet balance", description = "Retrieves the user wallet balance for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User Wallet balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User or wallet not found")
    })
    public ResponseEntity<UserWalletBalanceDTO> getUserWalletBalance(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletBalance(userId));
    }
}
