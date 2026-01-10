package com.questevent.controller;

import com.questevent.dto.UserWalletBalanceDTO;
import com.questevent.service.UserWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/wallet")
@Tag(name = "User Wallets", description = "Authenticated user's wallet APIs")
public class UserWalletController {

    private static final Logger log = LoggerFactory.getLogger(UserWalletController.class);

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
    @ApiResponse(responseCode = "200", description = "Wallet retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<UserWalletBalanceDTO> getWalletBalance() {

        log.info("Fetching wallet balance for authenticated user");

        UserWalletBalanceDTO balanceDTO =
                userWalletService.getMyWalletBalance();

        log.debug("Wallet balance fetched: gems={}", balanceDTO.getGems());

        return ResponseEntity.ok(balanceDTO);
    }
}
