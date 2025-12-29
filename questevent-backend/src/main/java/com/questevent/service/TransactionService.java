package com.questevent.service;

import com.questevent.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface TransactionService {

    void creditGems(User user, int amount);
}
