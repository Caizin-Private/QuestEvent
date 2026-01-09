package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.ActivityRequestDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActivityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController activityController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(activityController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createActivity_success() throws Exception {
        UUID programId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivityName("Test Activity");
        requestDTO.setActivityDuration(60);
        requestDTO.setRewardGems(100);
        requestDTO.setIsCompulsory(true);

        Program program = new Program();
        program.setProgramId(programId);

        Activity activity = new Activity();
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Test Activity");
        activity.setActivityDuration(60);
        activity.setRewardGems(100L);
        activity.setIsCompulsory(true);
        activity.setProgram(program);
        activity.setCreatedAt(Instant.now());

        when(activityService.createActivity(eq(programId), any(ActivityRequestDTO.class)))
                .thenReturn(activity);

        mockMvc.perform(post("/api/programs/{programId}/activities", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityName").value("Test Activity"))
                .andExpect(jsonPath("$.rewardGems").value(100));
    }

    @Test
    void createActivity_programNotFound() throws Exception {
        UUID programId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivityName("Test Activity");

        when(activityService.createActivity(eq(programId), any(ActivityRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(post("/api/programs/{programId}/activities", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActivities_success() throws Exception {
        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        Activity activity1 = new Activity();
        activity1.setActivityId(UUID.randomUUID());
        activity1.setActivityName("Activity 1");
        activity1.setProgram(program);

        Activity activity2 = new Activity();
        activity2.setActivityId(UUID.randomUUID());
        activity2.setActivityName("Activity 2");
        activity2.setProgram(program);

        when(activityService.getActivitiesByProgramId(programId))
                .thenReturn(List.of(activity1, activity2));

        mockMvc.perform(get("/api/programs/{programId}/activities", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateActivity_success() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivityName("Updated Activity");
        requestDTO.setRewardGems(200);

        Program program = new Program();
        program.setProgramId(programId);

        Activity updatedActivity = new Activity();
        updatedActivity.setActivityId(activityId);
        updatedActivity.setActivityName("Updated Activity");
        updatedActivity.setRewardGems(200L);
        updatedActivity.setProgram(program);

        when(activityService.updateActivity(eq(programId), eq(activityId), any(ActivityRequestDTO.class)))
                .thenReturn(updatedActivity);

        mockMvc.perform(put("/api/programs/{programId}/activities/{activityId}", programId, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityName").value("Updated Activity"))
                .andExpect(jsonPath("$.rewardGems").value(200));
    }

    @Test
    void updateActivity_activityNotFound() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();

        when(activityService.updateActivity(eq(programId), eq(activityId), any(ActivityRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        mockMvc.perform(put("/api/programs/{programId}/activities/{activityId}", programId, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteActivity_success() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        doNothing().when(activityService).deleteActivity(programId, activityId);

        mockMvc.perform(delete("/api/programs/{programId}/activities/{activityId}", programId, activityId))
                .andExpect(status().isNoContent());

        verify(activityService, times(1))
                .deleteActivity(programId, activityId);
    }

    @Test
    void deleteActivity_activityNotFound() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"))
                .when(activityService).deleteActivity(programId, activityId);

        mockMvc.perform(delete("/api/programs/{programId}/activities/{activityId}", programId, activityId))
                .andExpect(status().isNotFound());
    }
}
