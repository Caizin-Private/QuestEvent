package com.questevent.service;

import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    }

    @Test
    void registerParticipantForActivity_success() {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setUserId(1L);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");
        activity.setRewardGems(100);

        ActivityRegistration savedRegistration = new ActivityRegistration();
        savedRegistration.setActivityRegistrationId(1L);
        savedRegistration.setActivity(activity);
        savedRegistration.setUser(user);
        savedRegistration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository.existsByActivity_ActivityIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any(ActivityRegistration.class)))
                .thenReturn(savedRegistration);

        ActivityRegistrationResponseDTO result = activityRegistrationService.registerParticipantForActivity(request);

        assertNotNull(result);
        assertEquals(1L, result.getActivityRegistrationId());
        assertEquals(1L, result.getActivityId());
        assertEquals(1L, result.getUserId());
        verify(activityRegistrationRepository, times(1)).save(any(ActivityRegistration.class));
    }

    @Test
    void registerParticipantForActivity_userNotFound() {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setUserId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(activityRegistrationRepository, never()).save(any());
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(999L);
        request.setUserId(1L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("Activity not found with id: 999", exception.getMessage());
        verify(activityRegistrationRepository, never()).save(any());
    }

    @Test
    void registerParticipantForActivity_alreadyRegistered() {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setUserId(1L);

        User user = new User();
        user.setUserId(1L);

        Activity activity = new Activity();
        activity.setActivityId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRegistrationRepository.existsByActivity_ActivityIdAndUser_UserId(1L, 1L))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.registerParticipantForActivity(request)
        );

        assertEquals("User already registered for this activity", exception.getMessage());
        verify(activityRegistrationRepository, never()).save(any());
    }

    @Test
    void getAllRegistrations_success() {
        Activity activity1 = new Activity();
        activity1.setActivityId(1L);
        activity1.setActivityName("Activity 1");
        User user1 = new User();
        user1.setUserId(1L);
        user1.setName("User 1");

        Activity activity2 = new Activity();
        activity2.setActivityId(2L);
        activity2.setActivityName("Activity 2");
        User user2 = new User();
        user2.setUserId(2L);
        user2.setName("User 2");

        ActivityRegistration reg1 = new ActivityRegistration();
        reg1.setActivityRegistrationId(1L);
        reg1.setActivity(activity1);
        reg1.setUser(user1);
        reg1.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistration reg2 = new ActivityRegistration();
        reg2.setActivityRegistrationId(2L);
        reg2.setActivity(activity2);
        reg2.setUser(user2);
        reg2.setCompletionStatus(CompletionStatus.COMPLETED);

        when(activityRegistrationRepository.findAll()).thenReturn(List.of(reg1, reg2));

        List<ActivityRegistrationDTO> result = activityRegistrationService.getAllRegistrations();

        assertEquals(2, result.size());
        verify(activityRegistrationRepository, times(1)).findAll();
    }

    @Test
    void getRegistrationById_success() {
        Long id = 1L;
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(id);

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findById(id)).thenReturn(Optional.of(registration));

        ActivityRegistrationDTO result = activityRegistrationService.getRegistrationById(id);

        assertNotNull(result);
        verify(activityRegistrationRepository, times(1)).findById(id);
    }

    @Test
    void getRegistrationById_notFound() {
        Long id = 999L;

        when(activityRegistrationRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.getRegistrationById(id)
        );

        assertEquals("Registration not found with id: 999", exception.getMessage());
    }

    @Test
    void getRegistrationsByActivityId_success() {
        Long activityId = 1L;
        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test Activity");
        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(1L);
        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findByActivityActivityId(activityId))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result = activityRegistrationService.getRegistrationsByActivityId(activityId);

        assertEquals(1, result.size());
        verify(activityRegistrationRepository, times(1)).findByActivityActivityId(activityId);
    }

    @Test
    void getRegistrationsByUserId_success() {
        Long userId = 1L;
        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");
        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(1L);
        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findByUserUserId(userId))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result = activityRegistrationService.getRegistrationsByUserId(userId);

        assertEquals(1, result.size());
        verify(activityRegistrationRepository, times(1)).findByUserUserId(userId);
    }

    @Test
    void getRegistrationsByActivityIdAndStatus_success() {
        Long activityId = 1L;
        CompletionStatus status = CompletionStatus.COMPLETED;
        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setActivityName("Test Activity");
        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(1L);
        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(status);

        when(activityRegistrationRepository.findByActivityActivityIdAndCompletionStatus(activityId, status))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result = activityRegistrationService.getRegistrationsByActivityIdAndStatus(activityId, status);

        assertEquals(1, result.size());
        verify(activityRegistrationRepository, times(1))
                .findByActivityActivityIdAndCompletionStatus(activityId, status);
    }

    @Test
    void getRegistrationsByUserIdAndStatus_success() {
        Long userId = 1L;
        CompletionStatus status = CompletionStatus.NOT_COMPLETED;
        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");
        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(1L);
        reg.setActivity(activity);
        reg.setUser(user);
        reg.setCompletionStatus(status);

        when(activityRegistrationRepository.findByUserUserIdAndCompletionStatus(userId, status))
                .thenReturn(List.of(reg));

        List<ActivityRegistrationDTO> result = activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status);

        assertEquals(1, result.size());
        verify(activityRegistrationRepository, times(1))
                .findByUserUserIdAndCompletionStatus(userId, status);
    }

    @Test
    void updateCompletionStatus_success() {
        Long id = 1L;
        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(id);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setActivityName("Test Activity");

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        registration.setActivity(activity);
        registration.setUser(user);

        ActivityRegistration updatedRegistration = new ActivityRegistration();
        updatedRegistration.setActivityRegistrationId(id);
        updatedRegistration.setCompletionStatus(CompletionStatus.COMPLETED);
        updatedRegistration.setActivity(activity);
        updatedRegistration.setUser(user);

        when(activityRegistrationRepository.findById(id)).thenReturn(Optional.of(registration));
        when(activityRegistrationRepository.save(any(ActivityRegistration.class)))
                .thenReturn(updatedRegistration);

        ActivityRegistrationDTO result = activityRegistrationService.updateCompletionStatus(id, updateDTO);

        assertNotNull(result);
        verify(activityRegistrationRepository, times(1)).save(any(ActivityRegistration.class));
    }

    @Test
    void updateCompletionStatus_notFound() {
        Long id = 999L;
        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();

        when(activityRegistrationRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.updateCompletionStatus(id, updateDTO)
        );

        assertEquals("Registration not found with id: 999", exception.getMessage());
        verify(activityRegistrationRepository, never()).save(any());
    }

    @Test
    void deleteRegistration_success() {
        Long id = 1L;

        when(activityRegistrationRepository.existsById(id)).thenReturn(true);
        doNothing().when(activityRegistrationRepository).deleteById(id);

        assertDoesNotThrow(() -> activityRegistrationService.deleteRegistration(id));

        verify(activityRegistrationRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteRegistration_notFound() {
        Long id = 999L;

        when(activityRegistrationRepository.existsById(id)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> activityRegistrationService.deleteRegistration(id)
        );

        assertEquals("Registration not found with id: 999", exception.getMessage());
        verify(activityRegistrationRepository, never()).deleteById(any());
    }

    @Test
    void getParticipantCountForActivity_success() {
        Long activityId = 1L;
        long count = 5L;

        when(activityRegistrationRepository.countByActivityActivityId(activityId)).thenReturn(count);

        long result = activityRegistrationService.getParticipantCountForActivity(activityId);

        assertEquals(count, result);
        verify(activityRegistrationRepository, times(1)).countByActivityActivityId(activityId);
    }
}
