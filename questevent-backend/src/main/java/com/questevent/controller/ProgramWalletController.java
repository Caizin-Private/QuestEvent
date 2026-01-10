package com.questevent.controller;

import com.questevent.dto.ProgramWalletBalanceDTO;
import com.questevent.service.ProgramWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/program-wallets")
@Tag(name = "Program Wallets", description = "Program wallet management APIs")
public class ProgramWalletController {

    private static final Logger log =
            LoggerFactory.getLogger(ProgramWalletController.class);

    private final ProgramWalletService programWalletService;

    public ProgramWalletController(ProgramWalletService programWalletService) {
        this.programWalletService = programWalletService;
    }

    @PreAuthorize("@rbac.canAccessProgramWallet(authentication, #programWalletId)")
    @GetMapping("/{programWalletId}")
    @Operation(
            summary = "Get program wallet balance",
            description = "Retrieves the balance of a specific program wallet by wallet ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet found"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ProgramWalletBalanceDTO> getProgramWalletBalanceByProgramWalletId(
            @Parameter(description = "Program Wallet ID (UUID)", required = true)
            @PathVariable UUID programWalletId
    ) {
        log.info("Fetching program wallet balance by walletId={}", programWalletId);

        ProgramWalletBalanceDTO balance =
                programWalletService.getWalletBalanceByWalletId(programWalletId);

        log.debug("Program wallet balance fetched: walletId={}, balance={}",
                programWalletId, balance.getGems());

        return ResponseEntity.ok(balance);
    }

    @PreAuthorize("@rbac.canAccessMyProgramWallet(authentication, #programId)")
    @GetMapping("/program/{programId}/me")
    @Operation(
            summary = "Get my program wallet",
            description = "Returns the authenticated user's wallet for a program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    public ResponseEntity<ProgramWalletBalanceDTO> getMyProgramWallet(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId
    ) {
        log.info("Fetching my program wallet for programId={}", programId);

        ProgramWalletBalanceDTO wallet =
                programWalletService.getMyProgramWallet(programId);

        log.debug("My program wallet fetched: programId={}, balance={}",
                programId, wallet.getGems());

        return ResponseEntity.ok(wallet);
    }

    @PreAuthorize("@rbac.canAccessProgramWalletsByProgram(authentication, #programId)")
    @GetMapping("/program/{programId}")
    @Operation(
            summary = "Get all program wallets by program ID",
            description = "Returns wallet balances of all users registered in a program"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No wallets found for the program")
    })
    public ResponseEntity<List<ProgramWalletBalanceDTO>> getProgramWalletsByProgramId(
            @Parameter(description = "Program ID", required = true)
            @PathVariable UUID programId
    ) {
        log.info("Fetching all program wallets for programId={}", programId);

        List<ProgramWalletBalanceDTO> wallets =
                programWalletService.getProgramWalletsByProgramId(programId);

        log.debug("Program wallets fetched: programId={}, count={}",
                programId, wallets.size());

        return ResponseEntity.ok(wallets);
    }
}
