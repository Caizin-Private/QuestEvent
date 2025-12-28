package com.questevent.repository;

import com.questevent.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<UserWallet, UUID> {
    Optional<UserWallet> findByUserUserId(Long userId);
}
