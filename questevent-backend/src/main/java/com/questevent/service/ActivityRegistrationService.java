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
import java.util.stream.Collectors;

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

        log.debug(
                "Register participant for activity requested | activityId={}",
                request.getActivityId()
        );

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            log.warn("Unauthorized activity registration attempt");
            throw new UnauthorizedException("Unauthorized");
        }

        Long userId = principal.userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found | userId={}", userId);
                    return new UserNotFoundException("User not found");
                });

        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> {
                    log.error("Activity not found | activityId={}", request.getActivityId());
                    return new ActivityNotFoundException("Activity not found");
                });

        validateCompulsoryActivities(activity, userId);

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(), userId)) {

            log.warn(
                    "Duplicate activity registration blocked | activityId={} | userId={}",
                    activity.getActivityId(),
                    userId
            );
            throw new ResourceConflictException(
                    "User already registered for this activity"
            );
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

        log.info(
                "User registered for activity | registrationId={} | activityId={} | userId={}",
                saved.getActivityRegistrationId(),
                activity.getActivityId(),
                userId
        );

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getAllRegistrations() {
        log.info("Fetching all activity registrations");
        return activityRegistrationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActivityRegistrationDTO getRegistrationById(UUID id) {

        log.debug("Fetching activity registration | registrationId={}", id);

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() -> {
                            log.warn("Registration not found | registrationId={}", id);
                            return new ResourceNotFoundException(
                                    "Registration not found"
                            );
                        });

        return mapToDTO(registration);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityId(UUID activityId) {

        log.debug("Fetching registrations by activity | activityId={}", activityId);

        return activityRegistrationRepository.findByActivityActivityId(activityId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserId(Long userId) {

        log.debug("Fetching registrations by user | userId={}", userId);

        return activityRegistrationRepository.findByUserUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByActivityIdAndStatus(
            UUID activityId,
            CompletionStatus status) {

        log.debug(
                "Fetching registrations by activity and status | activityId={} | status={}",
                activityId,
                status
        );

        return activityRegistrationRepository
                .findByActivityActivityIdAndCompletionStatus(activityId, status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationDTO> getRegistrationsByUserIdAndStatus(
            Long userId,
            CompletionStatus status) {

        log.debug(
                "Fetching registrations by user and status | userId={} | status={}",
                userId,
                status
        );

        return activityRegistrationRepository
                .findByUserUserIdAndCompletionStatus(userId, status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityRegistrationDTO updateCompletionStatus(
            UUID id,
            ActivityCompletionUpdateDTO updateDTO) {

        log.debug(
                "Updating completion status | registrationId={} | status={}",
                id,
                updateDTO.getCompletionStatus()
        );

        ActivityRegistration registration =
                activityRegistrationRepository.findById(id)
                        .orElseThrow(() -> {
                            log.warn("Registration not found | registrationId={}", id);
                            return new ResourceNotFoundException(
                                    "Registration not found"
                            );
                        });

        registration.setCompletionStatus(updateDTO.getCompletionStatus());

        ActivityRegistration updated =
                activityRegistrationRepository.save(registration);

        log.info(
                "Completion status updated | registrationId={} | status={}",
                id,
                updated.getCompletionStatus()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteRegistration(UUID id) {

        log.debug("Delete activity registration requested | registrationId={}", id);

        if (!activityRegistrationRepository.existsById(id)) {
            log.warn("Registration not found while deleting | registrationId={}", id);
            throw new ResourceNotFoundException("Registration not found");
        }

        activityRegistrationRepository.deleteById(id);

        log.info("Activity registration deleted | registrationId={}", id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForActivity(UUID activityId) {

        long count =
                activityRegistrationRepository.countByActivityActivityId(activityId);

        log.info(
                "Participant count fetched | activityId={} | count={}",
                activityId,
                count
        );

        return count;
    }

    private void validateCompulsoryActivities(Activity activity, Long userId) {

        if (Boolean.TRUE.equals(activity.getIsCompulsory())) {
            return;
        }

        Program program = activity.getProgram();
        if (program == null) {
            return;
        }

        UUID programId = program.getProgramId();

        List<Activity> compulsoryActivities =
                activityRepository.findByProgram_ProgramIdAndIsCompulsoryTrue(programId);

        for (Activity compulsory : compulsoryActivities) {

            boolean completed =
                    activityRegistrationRepository
                            .existsByActivity_ActivityIdAndUser_UserIdAndCompletionStatus(
                                    compulsory.getActivityId(),
                                    userId,
                                    CompletionStatus.COMPLETED
                            );

            if (!completed) {
                log.warn(
                        "Compulsory activity not completed | compulsoryActivityId={} | userId={}",
                        compulsory.getActivityId(),
                        userId
                );
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

        log.debug(
                "Admin adding participant to activity | activityId={} | userId={}",
                activityId,
                request.getUserId()
        );

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> {
                    log.error("Activity not found | activityId={}", activityId);
                    return new ActivityNotFoundException("Activity not found");
                });

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found | userId={}", request.getUserId());
                    return new UserNotFoundException("User not found");
                });

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(),
                        user.getUserId())) {

            log.warn(
                    "Duplicate activity registration blocked (admin) | activityId={} | userId={}",
                    activity.getActivityId(),
                    user.getUserId()
            );
            throw new ResourceConflictException(
                    "User already registered for this activity"
            );
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistration saved =
                activityRegistrationRepository.save(registration);

        log.info(
                "Participant added to activity | registrationId={} | activityId={} | userId={}",
                saved.getActivityRegistrationId(),
                activityId,
                user.getUserId()
        );

        return mapToResponseDTO(saved);
    }
}
