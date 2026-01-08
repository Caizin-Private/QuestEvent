package com.questevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.questevent.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User>findByUserId(UUID userId);
}
