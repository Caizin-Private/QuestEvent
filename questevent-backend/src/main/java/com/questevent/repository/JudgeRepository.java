package com.questevent.repository;

import com.questevent.entity.Judge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JudgeRepository extends JpaRepository<Judge, Long> {

    // Find judge by linked user
    Optional<Judge> findByUserUserId(Long userId);

    // Check if a user is already a judge
    boolean existsByUserUserId(Long userId);
}
