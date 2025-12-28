package com.questevent.controller;

import com.questevent.dto.ProgramWalletCreateRequest;
import com.questevent.entity.ProgramWallet;
import com.questevent.service.ProgramWalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/program-wallet")
public class ProgramWalletController {

    private final ProgramWalletService programWalletService;

    public ProgramWalletController(ProgramWalletService programWalletService) {
        this.programWalletService = programWalletService;
    }

    @PostMapping("/create")
    public ProgramWallet createProgramWallet(
            @RequestBody ProgramWalletCreateRequest request
    ) {
        return programWalletService.createWallet(
                request.getUserId(),
                request.getProgramId()
        );
    }
}
