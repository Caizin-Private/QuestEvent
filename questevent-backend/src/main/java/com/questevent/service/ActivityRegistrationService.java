package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Transactional
    public ActivityRegistrationResponseDTO registerParticipantForActivity(
            ActivityRegistrationRequestDTO request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException("User not found: " + userId));

        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Activity not found: " + request.getActivityId()));

        validateCompulsoryActivities(activity, userId);

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(), userId)) {
            throw new IllegalStateException("User already registered for this activity");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(
                request.getCompletionStatus() != null
                        ? request.getCompletionStatus()
                        : CompletionStatus.NOT_COMPLETED
        );

        return mapToResponseDTO(
                activityRegistrationRepository.save(registration)
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getAllRegistrations() {
        return activityRegistrationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityRegistrationDTO getRegistrationById(Long id) {
        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Registration not found: " + id));
        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityId(Long activityId) {
        return activityRegistrationRepository.findByActivityActivityId(activityId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserId(Long userId) {
        return activityRegistrationRepository.findByUserUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityIdAndStatus(
            Long activityId,
            CompletionStatus status) {

        return activityRegistrationRepository
                .findByActivityActivityIdAndCompletionStatus(activityId, status)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserIdAndStatus(
            Long userId,
            CompletionStatus status) {

        return activityRegistrationRepository
                .findByUserUserIdAndCompletionStatus(userId, status)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public ActivityRegistrationDTO updateCompletionStatus(
            Long id,
            ActivityCompletionUpdateDTO updateDTO) {

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Registration not found: " + id));

        registration.setCompletionStatus(updateDTO.getCompletionStatus());
        return mapToDTO(activityRegistrationRepository.save(registration));
    }

    @Transactional
    public void deleteRegistration(Long id) {
        if (!activityRegistrationRepository.existsById(id)) {
            throw new IllegalArgumentException("Registration not found: " + id);
        }
        activityRegistrationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForActivity(Long activityId) {
        return activityRegistrationRepository.countByActivityActivityId(activityId);
    }

    @Transactional
    public ActivityRegistrationResponseDTO addParticipantToActivity(
            Long activityId,
            AddParticipantInActivityRequestDTO request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + request.getUserId()));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Activity not found: " + activityId));

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activityId, user.getUserId())) {
            throw new IllegalStateException(
                    "User already registered for this activity");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistration saved =
                activityRegistrationRepository.save(registration);

        return mapToResponseDTO(saved);
    }

    private void validateCompulsoryActivities(Activity activity, Long userId) {

        if (!Boolean.TRUE.equals(activity.getIsCompulsory())) {
            return;
        }

        Program program = activity.getProgram();
        if (program == null) {
            return;
        }

        List<Activity> compulsoryActivities =
                activityRepository.findByProgram_ProgramIdAndIsCompulsoryTrue(
                        program.getProgramId());

        for (Activity compulsory : compulsoryActivities) {
            boolean completed =
                    activityRegistrationRepository
                            .existsByActivity_ActivityIdAndUser_UserIdAndCompletionStatus(
                                    compulsory.getActivityId(),
                                    userId,
                                    CompletionStatus.COMPLETED);

            if (!completed) {
                throw new IllegalStateException(
                        "Complete compulsory activity: " +
                                compulsory.getActivityName());
            }
        }
    }

    private ActivityRegistrationDTO mapToDTO(ActivityRegistration registration) {
        return new ActivityRegistrationDTO(
                registration.getActivityRegistrationId(),
                registration.getActivity().getActivityId(),
                registration.getActivity().getActivityName(),
                registration.getUser().getUserId(),
                registration.getUser().getName(),
                registration.getCompletionStatus()
        );
    }

    private ActivityRegistrationResponseDTO mapToResponseDTO(
            ActivityRegistration registration) {

        ActivityRegistrationResponseDTO dto = new ActivityRegistrationResponseDTO();
        dto.setActivityRegistrationId(registration.getActivityRegistrationId());
        dto.setActivityId(registration.getActivity().getActivityId());
        dto.setActivityName(registration.getActivity().getActivityName());
        dto.setUserId(registration.getUser().getUserId());
        dto.setUserName(registration.getUser().getName());
        dto.setUserEmail(registration.getUser().getEmail());
        dto.setCompletionStatus(registration.getCompletionStatus());
        dto.setRewardGems(registration.getActivity().getRewardGems());
        dto.setMessage("Successfully registered for activity");
        return dto;
    }
}
