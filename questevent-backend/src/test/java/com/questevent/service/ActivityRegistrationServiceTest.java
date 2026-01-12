package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.Role;
import com.questevent.exception.*;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityRegistrationServiceTest {

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityRegistrationService activityRegistrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

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

    /* =====================================================
       REGISTER PARTICIPANT
       ===================================================== */

    @Test
    void registerParticipantForActivity_success() {

        Long userId = 1L;
        UUID activityId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test Activity");
        activity.setRewardGems(100L);

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(registrationId);
        saved.setActivity(activity);
        saved.setUser(user);
        saved.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any())).thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.registerParticipantForActivity(request);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(activityId, result.getActivityId());
        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void registerParticipantForActivity_userNotFound() {

        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {

        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        ActivityNotFoundException ex = assertThrows(
                ActivityNotFoundException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("Activity not found", ex.getMessage());
    }

    @Test
    void registerParticipantForActivity_alreadyRegistered() {

        Long userId = 1L;
        UUID activityId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(true);

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User already registered for this activity", ex.getMessage());
    }


    @Test
    void registerParticipantForActivity_shouldThrowUnauthorized() {

        SecurityContextHolder.clearContext();

        ActivityRegistrationRequestDTO request =
                new ActivityRegistrationRequestDTO();
        request.setActivityId(UUID.randomUUID());

        assertThrows(
                UnauthorizedException.class,
                () -> activityRegistrationService
                        .registerParticipantForActivity(request)
        );
    }

    @Test
    void getAllRegistrations_success() {

        ActivityRegistration reg = new ActivityRegistration();
        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findAll())
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result =
                activityRegistrationService.getAllRegistrations();

        assertEquals(1, result.size());
    }

    @Test
    void getRegistrationById_success() {

        UUID registrationId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(registrationId);

        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Activity");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.COMPLETED);

        when(activityRegistrationRepository.findById(registrationId))
                .thenReturn(Optional.of(reg));

        ActivityRegistrationDTO dto =
                activityRegistrationService.getRegistrationById(registrationId);

        assertEquals(registrationId, dto.getActivityRegistrationId());
    }

    @Test
    void getRegistrationsByActivityId_success() {

        UUID activityId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository
                .findByActivityActivityId(activityId))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result =
                activityRegistrationService.getRegistrationsByActivityId(activityId);

        assertEquals(1, result.size());
    }

    @Test
    void getRegistrationsByUserId_success() {

        Long userId = 1L;

        ActivityRegistration reg = new ActivityRegistration();
        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(userId);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository
                .findByUserUserId(userId))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result =
                activityRegistrationService.getRegistrationsByUserId(userId);

        assertEquals(1, result.size());
    }

    @Test
    void getRegistrationsByActivityIdAndStatus_success() {

        UUID activityId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.COMPLETED);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndCompletionStatus(
                        activityId,
                        CompletionStatus.COMPLETED))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result =
                activityRegistrationService
                        .getRegistrationsByActivityIdAndStatus(
                                activityId,
                                CompletionStatus.COMPLETED
                        );

        assertEquals(1, result.size());
    }

    @Test
    void getRegistrationsByUserIdAndStatus_success() {

        Long userId = 1L;

        ActivityRegistration reg = new ActivityRegistration();
        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(userId);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.COMPLETED);

        when(activityRegistrationRepository
                .findByUserUserIdAndCompletionStatus(
                        userId,
                        CompletionStatus.COMPLETED))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result =
                activityRegistrationService
                        .getRegistrationsByUserIdAndStatus(
                                userId,
                                CompletionStatus.COMPLETED
                        );

        assertEquals(1, result.size());
    }


    @Test
    void updateCompletionStatus_success() {

        UUID registrationId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(registrationId);

        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findById(registrationId))
                .thenReturn(Optional.of(reg));
        when(activityRegistrationRepository.save(reg))
                .thenReturn(reg);

        ActivityCompletionUpdateDTO dto =
                new ActivityCompletionUpdateDTO();
        dto.setCompletionStatus(CompletionStatus.COMPLETED);

        ActivityRegistrationDTO result =
                activityRegistrationService
                        .updateCompletionStatus(registrationId, dto);

        assertEquals(CompletionStatus.COMPLETED, result.getCompletionStatus());
    }

    @Test
    void getParticipantCountForActivity_success() {

        UUID activityId = UUID.randomUUID();

        when(activityRegistrationRepository
                .countByActivityActivityId(activityId))
                .thenReturn(5L);

        long count =
                activityRegistrationService
                        .getParticipantCountForActivity(activityId);

        assertEquals(5L, count);
    }

    @Test
    void registerParticipantForActivity_shouldFail_whenCompulsoryNotCompleted() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID compulsoryId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ActivityRegistrationRequestDTO request =
                new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);

        Activity mainActivity = new Activity();
        mainActivity.setActivityId(activityId);
        mainActivity.setProgram(program);
        mainActivity.setIsCompulsory(false);

        Activity compulsory = new Activity();
        compulsory.setActivityId(compulsoryId);
        compulsory.setActivityName("Mandatory Task");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(mainActivity));
        when(activityRepository
                .findByProgramProgramIdAndIsCompulsoryTrue(programId))
                .thenReturn(List.of(compulsory));
        when(activityRegistrationRepository
                .existsByActivityActivityIdAndUserUserIdAndCompletionStatus(
                        compulsoryId,
                        userId,
                        CompletionStatus.COMPLETED))
                .thenReturn(false);

        assertThrows(
                InvalidOperationException.class,
                () -> activityRegistrationService
                        .registerParticipantForActivity(request)
        );
    }







    // -------------------- ADD PARTICIPANT --------------------

    @Test
    void addParticipantToActivity_success() {

        UUID activityId = UUID.randomUUID();
        Long targetUserId = 2L;
        UUID registrationId = UUID.randomUUID();

        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(targetUserId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);

        User user = new User();
        user.setUserId(targetUserId);
        user.setName("Target User");

        ActivityRegistration saved = new ActivityRegistration();
        saved.setActivityRegistrationId(registrationId);
        saved.setActivity(activity);
        saved.setUser(user);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(activityRegistrationRepository
                .existsByActivityActivityIdAndUserUserId(activityId, targetUserId))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any())).thenReturn(saved);

        ActivityRegistrationResponseDTO result =
                activityRegistrationService.addParticipantToActivity(activityId, request);

        assertNotNull(result);
        assertEquals(targetUserId, result.getUserId());
        verify(activityRegistrationRepository).save(any());
    }

    // -------------------- DELETE --------------------

    @Test
    void deleteRegistration_success() {

        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                activityRegistrationService.deleteRegistration(registrationId));
    }

    @Test
    void deleteRegistration_notFound() {

        UUID registrationId = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(registrationId))
                .thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> activityRegistrationService.deleteRegistration(registrationId)
        );

        assertEquals("Registration not found", ex.getMessage());
    }
}
