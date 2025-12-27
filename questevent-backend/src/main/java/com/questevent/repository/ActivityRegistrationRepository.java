package com.questevent.repository;

import com.questevent.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRegistrationRepository
        extends JpaRepository<ActivityRegistration, Long> {
}
