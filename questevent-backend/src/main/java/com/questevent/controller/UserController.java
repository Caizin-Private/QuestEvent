package com.questevent.controller;

import com.questevent.entity.User;
import com.questevent.service.UserService;
import com.questevent.service.WalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final WalletService walletService;

    public UserController(UserService userService,
                          WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    @PostMapping
    public String createUserWithWallet(@RequestBody User user) {

        User savedUser = userService.createTestUser(user);

        walletService.createWalletForUser(savedUser);

        return "User created with userId = " + savedUser.getUserId()
                + " and wallet created successfully";
    }
}
