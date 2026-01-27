package com.questevent.service;

import com.questevent.dto.*;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ActivitySubmission;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.CompletionStatus;
import com.questevent.enums.ReviewStatus;
import com.questevent.exception.*;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final UserRepository userRepository;
    private final SecurityUserResolver securityUserResolver; // ✅ added

    @Transactional
    public ActivityRegistrationResponseDTO registerParticipantForActivity(
            ActivityRegistrationRequestDTO request) {

        log.debug(
                "Register participant for activity requested | activityId={}",
                request.getActivityId()
        );

        User user = securityUserResolver.getCurrentUser();
        Long userId = user.getUserId();

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
        registration.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

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
                                new ResourceNotFoundException("Registration not found")
                        );

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
                                new ResourceNotFoundException("Registration not found")
                        );

        registration.setCompletionStatus(updateDTO.getCompletionStatus());

        return mapToDTO(
                activityRegistrationRepository.save(registration)
        );
    }

    @Transactional
    public void deleteRegistration(UUID id) {

        if (!activityRegistrationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Registration not found");
        }

        activityRegistrationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getParticipantCountForActivity(UUID activityId) {

        return activityRegistrationRepository
                .countByActivityActivityId(activityId);
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
                activityRepository
                        .findByProgram_ProgramIdAndIsCompulsoryTrue(
                                program.getProgramId()
                        );

        for (Activity compulsory : compulsoryActivities) {

            // Check if user has registered for the compulsory activity
            boolean registered =
                    activityRegistrationRepository
                            .existsByActivity_ActivityIdAndUser_UserId(
                                    compulsory.getActivityId(),
                                    userId
                            );

            if (!registered) {
                throw new InvalidOperationException(
                        "Register for compulsory activity '"
                                + compulsory.getActivityName()
                                + "' before registering for this activity"
                );
            }

            // Check if the registration is completed
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

            // Check if the submission exists and is approved by judge
            ActivitySubmission submission =
                    activitySubmissionRepository
                            .findUserSubmissionForActivity(
                                    compulsory.getActivityId(),
                                    userId
                            )
                            .orElse(null);

            if (submission == null || submission.getReviewStatus() != ReviewStatus.APPROVED) {
                throw new InvalidOperationException(
                        "Compulsory activity must be approved by a judge before registering for this activity"
                );
            }
        }
    }

    @Transactional
    public ActivityRegistrationResponseDTO addParticipantToActivity(
            UUID activityId,
            AddParticipantInActivityRequestDTO request
    ) {

        log.debug(
                "Admin adding participant to activity | activityId={} | userId={}",
                activityId,
                request.getUserId()
        );

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() ->
                        new ActivityNotFoundException("Activity not found")
                );

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(
                        activity.getActivityId(),
                        user.getUserId())) {

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
}

