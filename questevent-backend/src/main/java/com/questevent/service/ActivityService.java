package com.questevent.service;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ProgramRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.questevent.exception.ActivityNotFoundException;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;


import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ActivityService {

    private static final String PROGRAM_NOT_FOUND = "Program not found";
    private final ActivityRepository activityRepository;
    private final ProgramRepository programRepository;

    public ActivityService(
            ActivityRepository activityRepository,
            ProgramRepository programRepository
    ) {
        this.activityRepository = activityRepository;
        this.programRepository = programRepository;
    }

    public Activity createActivity(UUID programId, ActivityRequestDTO dto) {

        log.debug("Create activity requested | programId={}", programId);

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> {
                    log.error("Program not found while creating activity | programId={}", programId);
                    return new ProgramNotFoundException(PROGRAM_NOT_FOUND );

                });

        Activity activity = new Activity();
        mapDtoToEntity(dto, activity);
        activity.setProgram(program);

        Activity saved = activityRepository.save(activity);

        log.info(
                "Activity created | activityId={} | programId={}",
                saved.getActivityId(),
                programId
        );

        return saved;
    }

    public Activity updateActivity(UUID programId, UUID activityId, ActivityRequestDTO dto) {

        log.debug(
                "Update activity requested | programId={} | activityId={}",
                programId,
                activityId
        );

        Activity existingActivity = activityRepository.findById(activityId)
                .orElseThrow(() -> {
                    log.error("Activity not found | activityId={}", activityId);
                    return new ActivityNotFoundException("Activity not found");
                });

        if (!existingActivity.getProgram().getProgramId().equals(programId)) {
            log.warn(
                    "Activity-program mismatch | activityId={} | expectedProgramId={} | actualProgramId={}",
                    activityId,
                    programId,
                    existingActivity.getProgram().getProgramId()
            );
            throw new ResourceConflictException("Activity does not belong to this program");
        }

        mapDtoToEntity(dto, existingActivity);

        Activity updated = activityRepository.save(existingActivity);

        log.info(
                "Activity updated | activityId={} | programId={}",
                activityId,
                programId
        );

        return updated;
    }

    public List<Activity> getActivitiesByProgramId(UUID programId) {

        log.debug("Fetching activities by program | programId={}", programId);

        if (!programRepository.existsById(programId)) {
            log.warn("Program not found while fetching activities | programId={}", programId);
            throw new ProgramNotFoundException(PROGRAM_NOT_FOUND );
        }

        List<Activity> activities =
                activityRepository.findByProgram_ProgramId(programId);

        log.info(
                "Activities fetched | programId={} | count={}",
                programId,
                activities.size()
        );

        return activities;
    }

    public void deleteActivity(UUID programId, UUID activityId) {

        log.debug(
                "Delete activity requested | programId={} | activityId={}",
                programId,
                activityId
        );

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> {
                    log.warn("Activity not found while deleting | activityId={}", activityId);
                    return new ActivityNotFoundException("Activity not found");
                });

        if (!activity.getProgram().getProgramId().equals(programId)) {
            log.warn(
                    "Delete forbidden due to program mismatch | activityId={} | programId={}",
                    activityId,
                    programId
            );
            throw new ResourceConflictException("Activity does not belong to this program");
        }

        activityRepository.delete(activity);

        log.info(
                "Activity deleted | activityId={} | programId={}",
                activityId,
                programId
        );
    }

    public List<Activity> getCompulsoryActivitiesByProgramId(UUID programId) {

        log.debug(
                "Fetching compulsory activities | programId={}",
                programId
        );

        if (!programRepository.existsById(programId)) {
            log.warn(
                    "Program not found while fetching compulsory activities | programId={}",
                    programId
            );
            throw new ProgramNotFoundException(PROGRAM_NOT_FOUND );
        }

        List<Activity> activities =
                activityRepository
                        .findByProgram_ProgramIdAndIsCompulsoryTrue(programId);

        log.info(
                "Compulsory activities fetched | programId={} | count={}",
                programId,
                activities.size()
        );

        return activities;
    }

    private void mapDtoToEntity(ActivityRequestDTO dto, Activity activity) {
        activity.setActivityName(dto.getActivityName());
        activity.setActivityDuration(dto.getActivityDuration());
        activity.setActivityRulebook(dto.getActivityRulebook());
        activity.setActivityDescription(dto.getActivityDescription());
        activity.setRewardGems(Long.valueOf(dto.getRewardGems()));
        activity.setIsCompulsory(dto.getIsCompulsory());
    }
}
