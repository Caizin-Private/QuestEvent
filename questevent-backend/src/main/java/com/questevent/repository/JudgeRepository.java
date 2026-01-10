package com.questevent.repository;

import com.questevent.entity.Judge;
import com.questevent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JudgeRepository extends JpaRepository<Judge, UUID> {

    // Find judge by linked user
    Optional<Judge> findByUserUserId(Long userId);

    // Check if a user is already a judge
    boolean existsByUserUserId(Long userId);

    Optional<Judge> findByUser(User user);
}
