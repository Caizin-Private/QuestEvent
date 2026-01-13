package com.questevent.service;

import com.questevent.entity.User;
import com.questevent.entity.UserWallet;
import com.questevent.enums.Department;
import com.questevent.exception.UserNotAuthenticatedException;
import com.questevent.repository.UserRepository;
import com.questevent.repository.UserWalletRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;

    public AuthService(UserRepository userRepository,
                       UserWalletRepository userWalletRepository) {
        this.userRepository = userRepository;
        this.userWalletRepository = userWalletRepository;
    }

    public boolean isLoggedIn(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        boolean loggedIn =
                session != null && session.getAttribute(SESSION_USER_ID) != null;

        log.debug("Checking login status: {}", loggedIn);
        return loggedIn;
    }

    public Long getLoggedInUserId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(SESSION_USER_ID) == null) {
            log.warn("Unauthorized access attempt — no session or userId");
            throw new UserNotAuthenticatedException();
        }

        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        log.debug("Retrieved session userId={}", userId);

        return userId;
    }

    public User getLoggedInUser(HttpServletRequest request) {

        Long userId = getLoggedInUserId(request);

        log.info("Fetching logged-in user details userId={}", userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId={}", userId);
                    return new RuntimeException("User not found");
                });
    }

    public void completeProfile(Long userId,
                                Department department,
                                String gender) {

        log.info("Completing profile userId={}, department={}, gender={}",
                userId, department, gender);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found while completing profile userId={}",
                            userId);
                    return new RuntimeException("User not found");
                });

        user.setDepartment(department);
        user.setGender(gender);
        userRepository.save(user);

        log.debug("User profile updated successfully userId={}", userId);

        userWalletRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    log.info("Wallet not found, creating wallet userId={}",
                            userId);
                    UserWallet wallet = new UserWallet();
                    wallet.setUser(user);
                    wallet.setGems(0L);
                    return userWalletRepository.save(wallet);
                });
    }
}
