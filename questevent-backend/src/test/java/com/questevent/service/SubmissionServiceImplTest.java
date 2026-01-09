package com.questevent.service;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.Role;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitActivity_shouldSaveSubmissionSuccessfully() {

        mockAuthenticatedUser(2L);

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(registration.getUser()));

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(1L))
                .thenReturn(false);

        submissionService.submitActivity(
                1L,
                "https://github.com/project"
        );

        verify(submissionRepository).save(any(ActivitySubmission.class));
        verify(registrationRepository).save(registration);

        assertEquals(
                CompletionStatus.COMPLETED,
                registration.getCompletionStatus()
        );
    }

    @Test
    void submitActivity_shouldThrowIfUserNotRegistered() {

        mockAuthenticatedUser(2L);

        User user = new User();
        user.setUserId(2L); // ✅ THIS IS CRITICAL

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(1L, "url")
        );

        assertEquals(
                "User is not registered for this activity",
                ex.getMessage()
        );
    }


    @Test
    void submitActivity_shouldThrowIfActivityAlreadyCompleted() {

        mockAuthenticatedUser(2L);

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.COMPLETED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(registration.getUser()));

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(1L, "url")
        );

        assertEquals(
                "Activity already completed. Submission not allowed.",
                ex.getMessage()
        );
    }

    @Test
    void submitActivity_shouldThrowIfSubmissionAlreadyExists() {

        mockAuthenticatedUser(2L);

        ActivityRegistration registration = mockRegistration();
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(registration.getUser()));

        when(registrationRepository
                .findByActivityActivityIdAndUserUserId(1L, 2L))
                .thenReturn(Optional.of(registration));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(1L))
                .thenReturn(true);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> submissionService.submitActivity(1L, "url")
        );

        assertEquals(
                "Submission already exists for this registration",
                ex.getMessage()
        );
    }

    private ActivityRegistration mockRegistration() {

        User user = new User();
        user.setUserId(2L);
        user.setName("Test User");

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Hackathon");

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(1L);
        registration.setUser(user);
        registration.setActivity(activity);

        return registration;
    }
}
