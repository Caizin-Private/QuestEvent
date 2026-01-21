package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.ActivityRequestDTO;
import com.questevent.dto.ActivityWithRegistrationStatusDTO;
import com.questevent.entity.Activity;
import com.questevent.entity.Program;
import com.questevent.exception.ActivityNotFoundException;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.rbac.RbacService;
import com.questevent.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityService activityService;

    // IMPORTANT: must match @rbac in @PreAuthorize
    @MockBean(name = "rbac")
    private RbacService rbac;

    // -------------------------------
    // 1️⃣ CREATE ACTIVITY – SUCCESS
    // -------------------------------
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

        when(activityService.createActivity(eq(programId), any()))
                .thenReturn(activity);

        mockMvc.perform(
                        post("/api/programs/{programId}/activities", programId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityName").value("Test Activity"))
                .andExpect(jsonPath("$.rewardGems").value(100));
    }

    // --------------------------------
    // 2️⃣ CREATE ACTIVITY – NOT FOUND
    // --------------------------------
    @Test
    void createActivity_programNotFound() throws Exception {
        UUID programId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivityName("Test Activity");

        when(activityService.createActivity(eq(programId), any()))
                .thenThrow(new ProgramNotFoundException("Program not found"));

        mockMvc.perform(
                        post("/api/programs/{programId}/activities", programId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO))
                )
                .andExpect(status().isNotFound());
    }

    // -------------------------------
    // 3️⃣ GET ACTIVITIES – SUCCESS
    // -------------------------------
    @Test
    void getActivities_success() throws Exception {
        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        Activity a1 = new Activity();
        a1.setActivityId(UUID.randomUUID());
        a1.setActivityName("Activity 1");
        a1.setProgram(program);

        Activity a2 = new Activity();
        a2.setActivityId(UUID.randomUUID());
        a2.setActivityName("Activity 2");
        a2.setProgram(program);

        when(activityService.getActivitiesByProgramId(programId))
                .thenReturn(List.of(a1, a2));

        mockMvc.perform(
                        get("/api/programs/{programId}/activities", programId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // -------------------------------
    // 4️⃣ UPDATE ACTIVITY – SUCCESS
    // -------------------------------
    @Test
    void updateActivity_success() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivityName("Updated Activity");
        requestDTO.setRewardGems(200);

        Program program = new Program();
        program.setProgramId(programId);

        Activity updated = new Activity();
        updated.setActivityId(activityId);
        updated.setActivityName("Updated Activity");
        updated.setRewardGems(200L);
        updated.setProgram(program);

        when(activityService.updateActivity(eq(programId), eq(activityId), any()))
                .thenReturn(updated);

        mockMvc.perform(
                        put("/api/programs/{programId}/activities/{activityId}",
                                programId, activityId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityName").value("Updated Activity"))
                .andExpect(jsonPath("$.rewardGems").value(200));
    }

    // --------------------------------
    // 5️⃣ UPDATE ACTIVITY – NOT FOUND
    // --------------------------------
    @Test
    void updateActivity_activityNotFound() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        when(activityService.updateActivity(eq(programId), eq(activityId), any()))
                .thenThrow(new ActivityNotFoundException("Activity not found"));

        mockMvc.perform(
                        put("/api/programs/{programId}/activities/{activityId}",
                                programId, activityId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ActivityRequestDTO()))
                )
                .andExpect(status().isNotFound());
    }

    // -------------------------------
    // 6️⃣ DELETE ACTIVITY – SUCCESS
    // -------------------------------
    @Test
    void deleteActivity_success() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        doNothing().when(activityService)
                .deleteActivity(programId, activityId);

        mockMvc.perform(
                        delete("/api/programs/{programId}/activities/{activityId}",
                                programId, activityId)
                )
                .andExpect(status().isNoContent());

        verify(activityService)
                .deleteActivity(programId, activityId);
    }

    // --------------------------------
    // 7️⃣ DELETE ACTIVITY – NOT FOUND
    // --------------------------------
    @Test
    void deleteActivity_activityNotFound() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        doThrow(new ActivityNotFoundException("Activity not found"))
                .when(activityService)
                .deleteActivity(programId, activityId);

        mockMvc.perform(
                        delete("/api/programs/{programId}/activities/{activityId}",
                                programId, activityId)
                )
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------
    // 8️⃣ GET ACTIVITIES FOR USER – USER VIEW
    // ---------------------------------------------
    @Test
    void getActivitiesForUser_success() throws Exception {
        UUID programId = UUID.randomUUID();

        ActivityWithRegistrationStatusDTO dto =
                new ActivityWithRegistrationStatusDTO(
                        UUID.randomUUID(),
                        "Hackathon",
                        false,
                        null
                );

        when(rbac.canViewProgram(any(), eq(programId)))
                .thenReturn(true);

        when(activityService.getActivitiesForUser(programId))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/programs/{programId}/activities/user-view", programId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityName").value("Hackathon"))
                .andExpect(jsonPath("$[0].isRegistered").value(false))
                .andExpect(jsonPath("$[0].completionStatus").doesNotExist());
    }
}


