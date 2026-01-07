package com.questevent.repository;

import com.questevent.entity.ActivityRegistration;
import com.questevent.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository
        extends JpaRepository<ActivityRegistration, Long> {

    boolean existsByActivity_ActivityIdAndUser_UserId(Long activityId, Long userId);

    List<ActivityRegistration> findByActivityActivityId(Long activityId);

    List<ActivityRegistration> findByUserUserId(Long userId);

    Optional<ActivityRegistration> findByActivityActivityIdAndUserUserId(Long activityId, Long userId);

    List<ActivityRegistration> findByActivityActivityIdAndCompletionStatus(Long activityId, CompletionStatus status);

    List<ActivityRegistration> findByUserUserIdAndCompletionStatus(Long userId, CompletionStatus status);

    long countByActivityActivityId(Long activityId);

    boolean existsByActivity_ActivityIdAndUser_UserIdAndCompletionStatus(Long activityId, Long userId, CompletionStatus completionStatus);
}

