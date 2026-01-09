package com.questevent.repository;

import com.questevent.entity.Judge;
import com.questevent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JudgeRepository extends JpaRepository<Judge, UUID> {


    Optional<Judge> findByUserUserId(Long userId);


    boolean existsByUserUserId(Long userId);

    Optional<Judge> findByUser(User user);
}
