package com.questevent.repository;

import com.questevent.entity.AllowedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AllowedUserRepository extends JpaRepository<AllowedUser, Long> {
    Optional<AllowedUser> findByEmail(String email);
}