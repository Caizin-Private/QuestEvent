package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
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
        mockMvc = MockMvcBuilders.standaloneSetup(activityRegistrationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerParticipant_success() throws Exception {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setUserId(1L);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistrationResponseDTO response = new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(1L);
        response.setActivityId(1L);
        response.setUserId(1L);
        response.setMessage("Successfully registered for activity");

        when(activityRegistrationService.registerParticipantForActivity(any(ActivityRegistrationRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/activity-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityRegistrationId").value(1L))
                .andExpect(jsonPath("$.message").value("Successfully registered for activity"));
    }

    @Test
    void registerParticipant_alreadyRegistered() throws Exception {
        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(1L);
        request.setUserId(1L);

        when(activityRegistrationService.registerParticipantForActivity(any(ActivityRegistrationRequestDTO.class)))
                .thenThrow(new RuntimeException("User already registered for this activity"));

        mockMvc.perform(post("/api/activity-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllRegistrations_success() throws Exception {
        ActivityRegistrationDTO dto1 = new ActivityRegistrationDTO(1L, 1L, "Activity 1", 1L, "User 1", CompletionStatus.NOT_COMPLETED);
        ActivityRegistrationDTO dto2 = new ActivityRegistrationDTO(2L, 2L, "Activity 2", 2L, "User 2", CompletionStatus.COMPLETED);

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
        ActivityRegistrationDTO dto = new ActivityRegistrationDTO(id, 1L, "Activity 1", 1L, "User 1", CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationById(id))
                .thenReturn(dto);

        mockMvc.perform(get("/api/activity-registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityRegistrationId").value(id))
                .andExpect(jsonPath("$.activityName").value("Activity 1"));
    }

    @Test
    void getRegistrationById_notFound() throws Exception {
        Long id = 999L;

        when(activityRegistrationService.getRegistrationById(id))
                .thenThrow(new RuntimeException("Registration not found"));

        mockMvc.perform(get("/api/activity-registrations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationsByActivity_success() throws Exception {
        Long activityId = 1L;
        ActivityRegistrationDTO dto = new ActivityRegistrationDTO(1L, activityId, "Activity 1", 1L, "User 1", CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationsByActivityId(activityId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activity-registrations/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityId").value(activityId));
    }

    @Test
    void getRegistrationsByUser_success() throws Exception {
        Long userId = 1L;
        ActivityRegistrationDTO dto = new ActivityRegistrationDTO(1L, 1L, "Activity 1", userId, "User 1", CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationsByUserId(userId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activity-registrations/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getRegistrationsByActivityAndStatus_success() throws Exception {
        Long activityId = 1L;
        CompletionStatus status = CompletionStatus.COMPLETED;
        ActivityRegistrationDTO dto = new ActivityRegistrationDTO(1L, activityId, "Activity 1", 1L, "User 1", status);

        when(activityRegistrationService.getRegistrationsByActivityIdAndStatus(activityId, status))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activity-registrations/activities/{activityId}/status/{status}", activityId, status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].completionStatus").value(status.toString()));
    }

    @Test
    void getRegistrationsByUserAndStatus_success() throws Exception {
        Long userId = 1L;
        CompletionStatus status = CompletionStatus.NOT_COMPLETED;
        ActivityRegistrationDTO dto = new ActivityRegistrationDTO(1L, 1L, "Activity 1", userId, "User 1", status);

        when(activityRegistrationService.getRegistrationsByUserIdAndStatus(userId, status))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activity-registrations/users/{userId}/status/{status}", userId, status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].completionStatus").value(status.toString()));
    }

    @Test
    void updateCompletionStatus_success() throws Exception {
        Long id = 1L;
        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        ActivityRegistrationDTO updatedDTO = new ActivityRegistrationDTO(id, 1L, "Activity 1", 1L, "User 1", CompletionStatus.COMPLETED);

        when(activityRegistrationService.updateCompletionStatus(eq(id), any(ActivityCompletionUpdateDTO.class)))
                .thenReturn(updatedDTO);

        mockMvc.perform(patch("/api/activity-registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value(CompletionStatus.COMPLETED.toString()));
    }

    @Test
    void updateCompletionStatus_notFound() throws Exception {
        Long id = 999L;
        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        when(activityRegistrationService.updateCompletionStatus(eq(id), any(ActivityCompletionUpdateDTO.class)))
                .thenThrow(new RuntimeException("Registration not found"));

        mockMvc.perform(patch("/api/activity-registrations/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getParticipantCount_success() throws Exception {
        Long activityId = 1L;
        long count = 5L;

        when(activityRegistrationService.getParticipantCountForActivity(activityId))
                .thenReturn(count);

        mockMvc.perform(get("/api/activity-registrations/activities/{activityId}/count", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(count));
    }

    @Test
    void deleteRegistration_success() throws Exception {
        Long id = 1L;

        doNothing().when(activityRegistrationService).deleteRegistration(id);

        mockMvc.perform(delete("/api/activity-registrations/{id}", id))
                .andExpect(status().isNoContent());

        verify(activityRegistrationService, times(1)).deleteRegistration(id);
    }

    @Test
    void deleteRegistration_notFound() throws Exception {
        Long id = 999L;

        doThrow(new RuntimeException("Registration not found"))
                .when(activityRegistrationService).deleteRegistration(id);

        mockMvc.perform(delete("/api/activity-registrations/{id}", id))
                .andExpect(status().isNotFound());
    }
}
