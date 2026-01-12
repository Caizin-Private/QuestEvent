package com.questevent.service;

import com.questevent.dto.ActivityRequestDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.exception.ActivityNotFoundException;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

        ProgramNotFoundException exception = assertThrows(
                ProgramNotFoundException.class,
                () -> activityService.createActivity(programId, dto)
        );

        assertEquals("Program not found", exception.getMessage());
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

        ActivityNotFoundException exception = assertThrows(
                ActivityNotFoundException.class,
                () -> activityService.updateActivity(programId, activityId, dto)
        );

        assertEquals("Activity not found", exception.getMessage());
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

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> activityService.updateActivity(programId, activityId, dto)
        );

        assertEquals(
                "Activity does not belong to this program",
                exception.getMessage()
        );
        verify(activityRepository, never()).save(any());
    }

    @Test
    void getCompulsoryActivitiesByProgramId_success() {

        UUID programId = UUID.randomUUID();

        Activity a1 = new Activity();
        Activity a2 = new Activity();

        when(programRepository.existsById(programId))
                .thenReturn(true);

        when(activityRepository
                .findByProgramProgramIdAndIsCompulsoryTrue(programId))
                .thenReturn(List.of(a1, a2));

        List<Activity> result =
                activityService.getCompulsoryActivitiesByProgramId(programId);

        assertEquals(2, result.size());

        verify(programRepository).existsById(programId);
        verify(activityRepository)
                .findByProgramProgramIdAndIsCompulsoryTrue(programId);
    }

    @Test
    void getCompulsoryActivitiesByProgramId_programNotFound() {

        UUID programId = UUID.randomUUID();

        when(programRepository.existsById(programId))
                .thenReturn(false);

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> activityService.getCompulsoryActivitiesByProgramId(programId)
        );

        assertEquals("Program not found", ex.getMessage());

        verify(activityRepository, never())
                .findByProgramProgramIdAndIsCompulsoryTrue(any());
    }

    @Test
    void getActivitiesByProgramId_emptyList() {

        UUID programId = UUID.randomUUID();

        when(programRepository.existsById(programId))
                .thenReturn(true);

        when(activityRepository.findByProgramProgramId(programId))
                .thenReturn(List.of());

        List<Activity> result =
                activityService.getActivitiesByProgramId(programId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createActivity_mapsAllFieldsCorrectly() {

        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        ActivityRequestDTO dto = new ActivityRequestDTO();
        dto.setActivityName("Rules Test");
        dto.setActivityDuration(30);
        dto.setRewardGems(50);
        dto.setActivityRulebook("Rules");
        dto.setActivityDescription("Desc");
        dto.setIsCompulsory(false);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        when(activityRepository.save(any(Activity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Activity result =
                activityService.createActivity(programId, dto);

        assertEquals("Rules Test", result.getActivityName());
        assertEquals(30, result.getActivityDuration());
        assertEquals(50L, result.getRewardGems());
        assertEquals("Rules", result.getActivityRulebook());
        assertEquals("Desc", result.getActivityDescription());
        assertFalse(result.getIsCompulsory());
    }

    @Test
    void deleteActivity_programMatchesExactly_success() {

        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        Activity activity = new Activity();
        activity.setActivityId(activityId);
        activity.setProgram(program);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        activityService.deleteActivity(programId, activityId);

        verify(activityRepository).delete(activity);
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

        when(activityRepository.findByProgramProgramId(programId))
                .thenReturn(List.of(activity1, activity2));

        List<Activity> result =
                activityService.getActivitiesByProgramId(programId);

        assertEquals(2, result.size());

        verify(programRepository).existsById(programId);
        verify(activityRepository).findByProgramProgramId(programId);
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

        ActivityNotFoundException exception = assertThrows(
                ActivityNotFoundException.class,
                () -> activityService.deleteActivity(programId, activityId)
        );

        assertEquals("Activity not found", exception.getMessage());
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

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> activityService.deleteActivity(programId, activityId)
        );

        assertEquals(
                "Activity does not belong to this program",
                exception.getMessage()
        );
        verify(activityRepository, never()).delete(any());
    }
}
