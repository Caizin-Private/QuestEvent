package com.questevent.repository;

import com.questevent.entity.ActivityRegistration;
import com.questevent.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRegistrationRepository
        extends JpaRepository<ActivityRegistration, UUID> {

    boolean existsByActivityActivityIdAndUserUserId(UUID activityId, Long userId);

    List<ActivityRegistration> findByActivityActivityId(UUID activityId);

    List<ActivityRegistration> findByUserUserId(Long userId);

    Optional<ActivityRegistration> findByActivityActivityIdAndUserUserId(UUID activityId, Long userId);

    List<ActivityRegistration> findByActivityActivityIdAndCompletionStatus(UUID activityId, CompletionStatus status);

    List<ActivityRegistration> findByUserUserIdAndCompletionStatus(Long userId, CompletionStatus status);

    long countByActivityActivityId(UUID activityId);

    boolean existsByActivityActivityIdAndUserUserIdAndCompletionStatus(UUID activityId, Long userId, CompletionStatus completionStatus);
}

