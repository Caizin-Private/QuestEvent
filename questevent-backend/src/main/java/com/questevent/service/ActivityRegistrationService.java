package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Transactional
    public ActivityRegistrationResponseDTO registerParticipantForActivity(
            ActivityRegistrationRequestDTO request) {

        UserPrincipal principal =
                (UserPrincipal) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() ->
                        new RuntimeException("Activity not found"));

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(), userId)) {
            throw new RuntimeException("User already registered for this activity");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(
                request.getCompletionStatus() != null
                        ? request.getCompletionStatus()
                        : CompletionStatus.NOT_COMPLETED
        );

        ActivityRegistration saved = activityRegistrationRepository.save(registration);

        return mapToResponseDTO(saved);
    }




    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getAllRegistrations() {
        return activityRegistrationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActivityRegistrationDTO getRegistrationById(UUID id) {
        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Registration not found"));
        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityId(UUID activityId) {
        return activityRegistrationRepository.findByActivityActivityId(activityId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserId(Long userId) {
        return activityRegistrationRepository.findByUserUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityIdAndStatus(
            UUID activityId, CompletionStatus status) {

        return activityRegistrationRepository
                .findByActivityActivityIdAndCompletionStatus(activityId, status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserIdAndStatus(
            Long userId, CompletionStatus status) {

        return activityRegistrationRepository
                .findByUserUserIdAndCompletionStatus(userId, status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Transactional
    public ActivityRegistrationDTO updateCompletionStatus(
            UUID id, ActivityCompletionUpdateDTO updateDTO) {

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Registration not found"));

        registration.setCompletionStatus(updateDTO.getCompletionStatus());
        return mapToDTO(activityRegistrationRepository.save(registration));
    }

    // -------------------- DELETE --------------------

    @Transactional
    public void deleteRegistration(UUID id) {
        if (!activityRegistrationRepository.existsById(id)) {
            throw new RuntimeException("Registration not found");
        }
        activityRegistrationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForActivity(UUID activityId) {
        return activityRegistrationRepository.countByActivityActivityId(activityId);
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

    private ActivityRegistrationResponseDTO mapToResponseDTO(ActivityRegistration registration) {
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

    public ActivityRegistrationResponseDTO addParticipantToActivity(UUID activityId, AddParticipantInActivityRequestDTO request) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() ->
                        new RuntimeException("Activity not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(), user.getUserId())) {
            throw new RuntimeException("User already registered for this activity");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistration saved = activityRegistrationRepository.save(registration);

        return mapToResponseDTO(saved);
    }
}
