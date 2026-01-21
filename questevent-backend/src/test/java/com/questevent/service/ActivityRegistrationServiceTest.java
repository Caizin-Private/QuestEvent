package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.exception.*;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityRegistrationServiceTest {

    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private ActivityRegistrationService service;

    private User user;
    private Activity activity;
    private Program program;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@company.com");

        program = new Program();
        program.setProgramId(UUID.randomUUID());

        activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test Activity");
        activity.setProgram(program);
        activity.setIsCompulsory(true);
        activity.setRewardGems(50L);
    }

    @Test
    void registerParticipantForActivity_success() {
        ActivityRegistrationRequestDTO request =
                new ActivityRegistrationRequestDTO(activity.getActivityId());

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(activityRepository.findById(activity.getActivityId()))
                .thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(any(), any()))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistrationResponseDTO response =
                service.registerParticipantForActivity(request);

        assertThat(response.getUserId()).isEqualTo(user.getUserId());
        assertThat(response.getActivityId()).isEqualTo(activity.getActivityId());
        assertThat(response.getCompletionStatus())
                .isEqualTo(CompletionStatus.NOT_COMPLETED);

        verify(activityRegistrationRepository).save(any());
    }

    @Test
    void registerParticipantForActivity_activityNotFound() {
        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(activityRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.registerParticipantForActivity(
                        new ActivityRegistrationRequestDTO(UUID.randomUUID())
                ))
                .isInstanceOf(ActivityNotFoundException.class);
    }

    @Test
    void registerParticipantForActivity_duplicateRegistration() {
        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(activityRepository.findById(activity.getActivityId()))
                .thenReturn(Optional.of(activity));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.registerParticipantForActivity(
                        new ActivityRegistrationRequestDTO(activity.getActivityId())
                ))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void compulsoryActivityNotCompleted_throwsException() {
        activity.setIsCompulsory(false);

        Activity compulsory = new Activity();
        compulsory.setActivityId(UUID.randomUUID());
        compulsory.setActivityName("Mandatory Task");

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(activityRepository.findById(activity.getActivityId()))
                .thenReturn(Optional.of(activity));
        when(activityRepository
                .findByProgram_ProgramIdAndIsCompulsoryTrue(program.getProgramId()))
                .thenReturn(List.of(compulsory));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserIdAndCompletionStatus(
                        compulsory.getActivityId(), user.getUserId(), CompletionStatus.COMPLETED))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.registerParticipantForActivity(
                        new ActivityRegistrationRequestDTO(activity.getActivityId())
                ))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void getRegistrationById_success() {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(UUID.randomUUID());
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationRepository.findById(registration.getActivityRegistrationId()))
                .thenReturn(Optional.of(registration));

        ActivityRegistrationDTO dto =
                service.getRegistrationById(registration.getActivityRegistrationId());

        assertThat(dto.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    void getRegistrationById_notFound() {
        when(activityRegistrationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getRegistrationById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCompletionStatus_success() {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityRegistrationId(UUID.randomUUID());
        registration.setActivity(activity);
        registration.setUser(user);

        when(activityRegistrationRepository.findById(any()))
                .thenReturn(Optional.of(registration));
        when(activityRegistrationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityCompletionUpdateDTO updateDTO =
                new ActivityCompletionUpdateDTO(CompletionStatus.COMPLETED);

        ActivityRegistrationDTO dto =
                service.updateCompletionStatus(registration.getActivityRegistrationId(), updateDTO);

        assertThat(dto.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
    }

    @Test
    void deleteRegistration_success() {
        UUID id = UUID.randomUUID();

        when(activityRegistrationRepository.existsById(id)).thenReturn(true);

        service.deleteRegistration(id);

        verify(activityRegistrationRepository).deleteById(id);
    }

    @Test
    void deleteRegistration_notFound() {
        when(activityRegistrationRepository.existsById(any()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.deleteRegistration(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getParticipantCountForActivity_success() {
        when(activityRegistrationRepository
                .countByActivityActivityId(activity.getActivityId()))
                .thenReturn(5L);

        long count = service.getParticipantCountForActivity(activity.getActivityId());

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void addParticipantToActivity_success() {
        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(user.getUserId());

        when(activityRepository.findById(activity.getActivityId()))
                .thenReturn(Optional.of(activity));
        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(any(), any()))
                .thenReturn(false);
        when(activityRegistrationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityRegistrationResponseDTO response =
                service.addParticipantToActivity(activity.getActivityId(), request);

        assertThat(response.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    void addParticipantToActivity_userNotFound() {
        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(99L);

        when(activityRepository.findById(any()))
                .thenReturn(Optional.of(activity));
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.addParticipantToActivity(
                        activity.getActivityId(),
                        request
                ))
                .isInstanceOf(UserNotFoundException.class);
    }

}
