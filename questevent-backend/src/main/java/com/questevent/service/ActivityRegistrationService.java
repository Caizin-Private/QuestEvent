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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private static final String REGISTRATION_NOT_FOUND = "Registration not found";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String ACTIVITY_NOT_FOUND = "Activity not found";
    private static final String DUPLICATE_REGISTRATION =
            "User already registered for this activity";

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
            throw new UnauthorizedException("Unauthorized");
        }

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new ActivityNotFoundException(ACTIVITY_NOT_FOUND));

        validateCompulsoryActivities(activity, userId);

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(), userId)) {
            throw new ResourceConflictException(DUPLICATE_REGISTRATION);
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(
                request.getCompletionStatus() != null
                        ? request.getCompletionStatus()
                        : CompletionStatus.NOT_COMPLETED
        );

        ActivityRegistration saved =
                activityRegistrationRepository.save(registration);

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getAllRegistrations() {
        return activityRegistrationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityRegistrationDTO getRegistrationById(UUID id) {

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(REGISTRATION_NOT_FOUND));

        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityId(UUID activityId) {
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
            UUID activityId,
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
            UUID id,
            ActivityCompletionUpdateDTO updateDTO) {

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(REGISTRATION_NOT_FOUND));

        registration.setCompletionStatus(updateDTO.getCompletionStatus());

        ActivityRegistration updated =
                activityRegistrationRepository.save(registration);

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteRegistration(UUID id) {

        if (!activityRegistrationRepository.existsById(id)) {
            throw new ResourceNotFoundException(REGISTRATION_NOT_FOUND);
        }

        activityRegistrationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForActivity(UUID activityId) {
        return activityRegistrationRepository.countByActivityActivityId(activityId);
    }

    private void validateCompulsoryActivities(Activity activity, Long userId) {

        if (Boolean.TRUE.equals(activity.getIsCompulsory())) {
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
                                    CompletionStatus.COMPLETED
                            );

            if (!completed) {
                throw new InvalidOperationException(
                        "Complete compulsory activity '"
                                + compulsory.getActivityName()
                                + "' before registering for this activity"
                );
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

        ActivityRegistrationResponseDTO dto =
                new ActivityRegistrationResponseDTO();

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

    @Transactional
    public ActivityRegistrationResponseDTO addParticipantToActivity(
            UUID activityId,
            AddParticipantInActivityRequestDTO request) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(ACTIVITY_NOT_FOUND));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(),
                        user.getUserId())) {
            throw new ResourceConflictException(DUPLICATE_REGISTRATION);
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistration saved =
                activityRegistrationRepository.save(registration);

        return mapToResponseDTO(saved);
    }
}
