package com.questevent.controller;

import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.service.UserWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/wallet")
@Tag(name = "User Wallets", description = "Authenticated user's wallet APIs")
public class UserWalletController {

    private final UserWalletService userWalletService;

    public UserWalletController(UserWalletService userWalletService) {
        this.userWalletService = userWalletService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(
            summary = "Get my wallet balance",
            description = "Returns wallet balance of the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<UserWalletBalanceDTO> getMyWalletBalance() {
        return ResponseEntity.ok(
                userWalletService.getMyWalletBalance()
        );
    }
}
