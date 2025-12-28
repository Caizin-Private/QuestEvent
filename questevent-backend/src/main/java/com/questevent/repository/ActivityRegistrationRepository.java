package com.questevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRepository, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

}
