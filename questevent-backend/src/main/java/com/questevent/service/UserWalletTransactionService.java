package com.questevent.service;

import com.questevent.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface UserWalletTransactionService {

    void creditGems(User user, int amount);
}
