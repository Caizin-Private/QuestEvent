package com.questevent.controller;

import com.questevent.dto.ProgramWalletBalanceDto;
import com.questevent.dto.ProgramWalletCreateRequest;
import com.questevent.entity.ProgramWallet;
import com.questevent.service.ProgramWalletService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/program-wallets")
public class ProgramWalletController {

    private final ProgramWalletService programWalletService;

    public ProgramWalletController(ProgramWalletService programWalletService) {
        this.programWalletService = programWalletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramWalletBalanceDto createProgramWallet(
            @RequestBody ProgramWalletCreateRequest request
    ) {
        ProgramWallet wallet = programWalletService.createWallet(
                request.getUserId(),
                request.getProgramId()
        );

        ProgramWalletBalanceDto dto = new ProgramWalletBalanceDto();
        dto.setProgramWalletId(wallet.getProgramWalletId());
        dto.setGems(wallet.getGems());
        return dto;
    }
    @GetMapping("/user/{userId}")
    public List<ProgramWalletBalanceDto> getUserWalletBalances(
            @PathVariable Long userId
    ) {
        return programWalletService.getUserProgramWalletBalances(userId);
    }

    @GetMapping("/{programWalletId}")
    public ProgramWalletBalanceDto getProgramWalletBalance(
            @PathVariable UUID programWalletId
    ) {
        return programWalletService.getWalletBalanceByWalletId(programWalletId);
    }
}
