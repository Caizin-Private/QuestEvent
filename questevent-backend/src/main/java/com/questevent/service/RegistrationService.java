package com.questevent.service;

public interface RegistrationService {

    void registerForProgram(Long programId, Long userId);

    void registerForActivity(Long activityId, Long userId);
}

