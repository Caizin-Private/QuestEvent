package com.questevent.service;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createActivity_success() {
        Long programId = 1L;

        Program program = new Program();
        program.setProgramId(programId);

        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setActivityName("Test Activity");
        dto.setActivityDuration(60);
        dto.setRewardGems(100);
        dto.setIsCompulsory(true);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        when(activityRepository.save(any(Activity.class)))
                .thenAnswer(invocation -> {
                    Activity activity = invocation.getArgument(0, Activity.class);
                    activity.setActivityId(1L);
                    return activity;
                });

        Activity result = activityService.createActivity(programId, dto);

        assertNotNull(result);
        assertEquals(1L, result.getActivityId());
        assertEquals("Test Activity", result.getActivityName());
        assertEquals(100, result.getRewardGems());
        assertEquals(program, result.getProgram());
    }

    @Test
    void createActivity_programNotFound() {
        when(programRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> activityService.createActivity(1L, new ActivityRequestDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void updateActivity_success() {
        Long programId = 1L;
        Long activityId = 1L;

        Program program = new Program();
        program.setProgramId(programId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setProgram(program);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        when(activityRepository.save(any(Activity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Activity.class));

        Activity result =
                activityService.updateActivity(programId, activityId, new ActivityRequestDTO());

        assertNotNull(result);
        assertEquals(activityId, result.getActivityId());
    }

    @Test
    void updateActivity_activityNotFound() {
        when(activityRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> activityService.updateActivity(1L, 1L, new ActivityRequestDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Activity not found", ex.getReason());
    }

    @Test
    void updateActivity_programMismatch() {
        Program otherProgram = new Program();
        otherProgram.setProgramId(2L);

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setProgram(otherProgram);

        when(activityRepository.findById(1L))
                .thenReturn(Optional.of(activity));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> activityService.updateActivity(1L, 1L, new ActivityRequestDTO())
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Activity does not belong to this program", ex.getReason());
    }

    @Test
    void getActivitiesByProgramId_success() {
        when(programRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.findByProgram_ProgramId(1L))
                .thenReturn(List.of(new Activity(), new Activity()));

        List<Activity> result = activityService.getActivitiesByProgramId(1L);

        assertEquals(2, result.size());
    }

    @Test
    void deleteActivity_mismatch() {
        Program otherProgram = new Program();
        otherProgram.setProgramId(2L);

        Activity activity = new Activity();
        activity.setActivityId(1L);
        activity.setProgram(otherProgram);

        when(activityRepository.findById(1L))
                .thenReturn(Optional.of(activity));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> activityService.deleteActivity(1L, 1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Mismatch between Program and Activity", ex.getReason());
    }
}
