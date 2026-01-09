package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.dto.AddParticipantInActivityRequestDTO;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActivityRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActivityRegistrationService activityRegistrationService;

    @InjectMocks
    private ActivityRegistrationController activityRegistrationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(activityRegistrationController)
                .build();
        objectMapper = new ObjectMapper();
    }


    @Test
    void registerParticipant_success() throws Exception {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistrationResponseDTO response = new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(1L);
        response.setActivityId(1L);
        response.setUserId(1L);
        response.setMessage("Successfully registered for activity");

        when(activityRegistrationService
                .registerParticipantForActivity(any(ActivityRegistrationRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/activity-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityRegistrationId").value(1L))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for activity"));
    }


    @Test
    void addParticipantToActivity_success() throws Exception {
        Long activityId = 1L;

        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(2L);

        ActivityRegistrationResponseDTO response =
                new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(1L);
        response.setActivityId(activityId);
        response.setUserId(2L);
        response.setMessage("Successfully registered for activity");

        when(activityRegistrationService
                .addParticipantToActivity(eq(activityId), any(AddParticipantInActivityRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/activity-registrations/activities/{activityId}/participants", activityId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityRegistrationId").value(1L))
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for activity"));
    }


    @Test
    void getAllRegistrations_success() throws Exception {
        ActivityRegistrationDTO dto1 =
                new ActivityRegistrationDTO(
                        1L, 1L, "Activity 1", 1L, "User 1",
                        CompletionStatus.NOT_COMPLETED);

        ActivityRegistrationDTO dto2 =
                new ActivityRegistrationDTO(
                        2L, 2L, "Activity 2", 2L, "User 2",
                        CompletionStatus.COMPLETED);

        when(activityRegistrationService.getAllRegistrations())
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/activity-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activityRegistrationId").value(1L))
                .andExpect(jsonPath("$[1].activityRegistrationId").value(2L));
    }

    @Test
    void getRegistrationById_success() throws Exception {
        Long id = 1L;

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        id, 1L, "Activity 1", 1L, "User 1",
                        CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationById(id))
                .thenReturn(dto);

        mockMvc.perform(get("/api/activity-registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityRegistrationId").value(id))
                .andExpect(jsonPath("$.activityName").value("Activity 1"));
    }

    @Test
    void getRegistrationById_notFound() throws Exception {
        when(activityRegistrationService.getRegistrationById(999L))
                .thenThrow(new RuntimeException("Registration not found"));

        mockMvc.perform(get("/api/activity-registrations/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationsByActivity_success() throws Exception {
        Long activityId = 1L;

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        1L, activityId, "Activity 1", 1L, "User 1",
                        CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationsByActivityId(activityId))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/activity-registrations/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityId").value(activityId));
    }

    @Test
    void getRegistrationsByUser_success() throws Exception {
        Long userId = 1L;

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        1L, 1L, "Activity 1", userId, "User 1",
                        CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationsByUserId(userId))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/activity-registrations/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void updateCompletionStatus_success() throws Exception {
        Long id = 1L;

        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        ActivityRegistrationDTO updated =
                new ActivityRegistrationDTO(
                        id, 1L, "Activity 1", 1L, "User 1",
                        CompletionStatus.COMPLETED);

        when(activityRegistrationService
                .updateCompletionStatus(eq(id), any(ActivityCompletionUpdateDTO.class)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/activity-registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus")
                        .value(CompletionStatus.COMPLETED.toString()));
    }

    @Test
    void updateCompletionStatus_notFound() throws Exception {
        when(activityRegistrationService
                .updateCompletionStatus(eq(999L), any()))
                .thenThrow(new RuntimeException("Registration not found"));

        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        mockMvc.perform(patch("/api/activity-registrations/{id}/status", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteRegistration_success() throws Exception {
        doNothing().when(activityRegistrationService).deleteRegistration(1L);

        mockMvc.perform(delete("/api/activity-registrations/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(activityRegistrationService, times(1))
                .deleteRegistration(1L);
    }

    @Test
    void deleteRegistration_notFound() throws Exception {
        doThrow(new RuntimeException("Registration not found"))
                .when(activityRegistrationService)
                .deleteRegistration(999L);

        mockMvc.perform(delete("/api/activity-registrations/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
