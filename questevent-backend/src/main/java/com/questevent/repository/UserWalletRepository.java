package com.questevent.repository;

import com.questevent.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {
    Optional<UserWallet> findByUserUserId(Long userId);

    @Query("""
        SELECT w FROM UserWallet w
        JOIN FETCH w.user
        ORDER BY w.gems DESC
    """)
    List<UserWallet> findGlobalLeaderboard();
}
