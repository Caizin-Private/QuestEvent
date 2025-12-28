package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createTestUser(User user) {
        return userRepository.save(user);
    }

}
