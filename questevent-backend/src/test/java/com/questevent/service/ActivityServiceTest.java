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
import java.util.UUID;

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
        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setActivityName("Test Activity");
        dto.setActivityDuration(60);
        dto.setRewardGems(100);
        dto.setIsCompulsory(true);

        Activity savedActivity = new Activity();
        savedActivity.setActivityId(UUID.randomUUID());
        savedActivity.setActivityName("Test Activity");
        savedActivity.setActivityDuration(60);
        savedActivity.setRewardGems(100L);
        savedActivity.setIsCompulsory(true);
        savedActivity.setProgram(program);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        when(activityRepository.save(any(Activity.class)))
                .thenReturn(savedActivity);

        Activity result = activityService.createActivity(programId, dto);

        assertNotNull(result);
        assertEquals("Test Activity", result.getActivityName());
        assertEquals(100, result.getRewardGems());
        assertEquals(program, result.getProgram());
        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    void createActivity_programNotFound() {
        UUID programId = UUID.randomUUID();
        ActivityRequestDTO dto = new ActivityRequestDTO();

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityService.createActivity(programId, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Program not found", exception.getReason());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void updateActivity_success() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        Activity existingActivity = new Activity();
        existingActivity.setActivityId(activityId);
        existingActivity.setActivityName("Old Name");
        existingActivity.setProgram(program);

        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setActivityName("New Name");
        dto.setRewardGems(200);

        Activity updatedActivity = new Activity();
        updatedActivity.setActivityId(activityId);
        updatedActivity.setActivityName("New Name");
        updatedActivity.setRewardGems(200L);
        updatedActivity.setProgram(program);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(existingActivity));

        when(activityRepository.save(any(Activity.class)))
                .thenReturn(updatedActivity);

        Activity result =
                activityService.updateActivity(programId, activityId, dto);

        assertNotNull(result);
        assertEquals("New Name", result.getActivityName());
        assertEquals(200, result.getRewardGems());
        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    void updateActivity_activityNotFound() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        ActivityRequestDTO dto = new ActivityRequestDTO();

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityService.updateActivity(programId, activityId, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Activity not found", exception.getReason());
    }

    @Test
    void updateActivity_activityDoesNotBelongToProgram() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID differentProgramId = UUID.randomUUID();

        Program differentProgram = new Program();
        differentProgram.setProgramId(differentProgramId);

        Activity existingActivity = new Activity();
        existingActivity.setActivityId(activityId);
        existingActivity.setProgram(differentProgram);

        ActivityRequestDTO dto = new ActivityRequestDTO();

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(existingActivity));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityService.updateActivity(programId, activityId, dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Activity does not belong to this program", exception.getReason());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void getActivitiesByProgramId_success() {
        UUID programId = UUID.randomUUID();

        Activity activity1 = new Activity();
        activity1.setActivityId(UUID.randomUUID());

        Activity activity2 = new Activity();
        activity2.setActivityId(UUID.randomUUID());

        when(programRepository.existsById(programId))
                .thenReturn(true);

        when(activityRepository.findByProgram_ProgramId(programId))
                .thenReturn(List.of(activity1, activity2));

        List<Activity> result =
                activityService.getActivitiesByProgramId(programId);

        assertEquals(2, result.size());

        verify(programRepository).existsById(programId);
        verify(activityRepository).findByProgram_ProgramId(programId);
    }

    @Test
    void deleteActivity_success() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setProgram(program);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        doNothing().when(activityRepository).delete(activity);

        assertDoesNotThrow(() ->
                activityService.deleteActivity(programId, activityId));

        verify(activityRepository).delete(activity);
    }

    @Test
    void deleteActivity_activityNotFound() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityService.deleteActivity(programId, activityId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Activity not found", exception.getReason());
        verify(activityRepository, never()).delete(any());
    }

    @Test
    void deleteActivity_mismatchBetweenProgramAndActivity() {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID differentProgramId = UUID.randomUUID();

        Program differentProgram = new Program();
        differentProgram.setProgramId(differentProgramId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setProgram(differentProgram);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> activityService.deleteActivity(programId, activityId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Mismatch between Program and Activity", exception.getReason());
        verify(activityRepository, never()).delete(any());
    }
}
