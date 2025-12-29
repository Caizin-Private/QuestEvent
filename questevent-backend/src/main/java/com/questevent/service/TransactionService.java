package com.questevent.service;

import com.questevent.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface TransactionService {

    void debitGems(User user, int amount);

    void creditGems(User user, int amount);
}
