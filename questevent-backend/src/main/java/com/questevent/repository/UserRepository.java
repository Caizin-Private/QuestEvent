package com.questevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.questevent.entity.User;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User>findByUserId(Long userId);
}
