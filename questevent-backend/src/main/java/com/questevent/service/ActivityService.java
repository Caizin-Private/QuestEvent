package com.questevent.service;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ProgramRepository programRepository;

    public Activity createActivity(Long programId, ActivityRequestDTO dto) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        Activity activity = new Activity();
        mapDtoToEntity(dto, activity);
        activity.setProgram(program);
        return activityRepository.save(activity);
    }

    public Activity updateActivity(Long programId, Long activityId, ActivityRequestDTO dto) {
        Activity existingActivity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!existingActivity.getProgram().getProgramId().equals(programId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity does not belong to this program");
        }

        mapDtoToEntity(dto, existingActivity);
        return activityRepository.save(existingActivity);
    }

    public List<Activity> getActivitiesByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found");
        }
        return activityRepository.findByProgram_ProgramId(programId);
    }

    public void deleteActivity(Long programId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!activity.getProgram().getProgramId().equals(programId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mismatch between Program and Activity");
        }
        activityRepository.delete(activity);
    }
    public List<Activity> getCompulsoryActivitiesByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found");
        }
        return activityRepository.findByProgram_ProgramIdAndIsCompulsoryTrue(programId);
    }

    private void mapDtoToEntity(ActivityRequestDTO dto, Activity activity) {
        activity.setActivityName(dto.getActivityName());
        activity.setActivityDuration(dto.getActivityDuration());
        activity.setActivityRulebook(dto.getActivityRulebook());
        activity.setActivityDescription(dto.getActivityDescription());
        activity.setRewardGems(dto.getRewardGems());
        activity.setIsCompulsory(dto.getIsCompulsory());
    }
}
