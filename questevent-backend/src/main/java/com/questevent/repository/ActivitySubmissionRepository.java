package com.questevent.repository;

import com.questevent.entity.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySubmissionRepository
        extends JpaRepository<ActivitySubmission, Long> {


    boolean existsByActivityRegistrationId(Long activityRegistrationId);



}