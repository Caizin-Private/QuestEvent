package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.ActivityCompletionUpdateDTO;
import com.questevent.dto.ActivityRegistrationDTO;
import com.questevent.dto.ActivityRegistrationRequestDTO;
import com.questevent.dto.ActivityRegistrationResponseDTO;
import com.questevent.dto.AddParticipantInActivityRequestDTO;
import com.questevent.enums.CompletionStatus;
import com.questevent.exception.ResourceNotFoundException;
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
import java.util.UUID;

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
        UUID activityId = UUID.randomUUID();

        ActivityRegistrationRequestDTO request = new ActivityRegistrationRequestDTO();
        request.setActivityId(activityId);
        request.setCompletionStatus(CompletionStatus.NOT_COMPLETED);

        ActivityRegistrationResponseDTO response = new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(UUID.randomUUID());
        response.setActivityId(activityId);
        response.setUserId(1L);
        response.setMessage("Successfully registered for activity");

        when(activityRegistrationService
                .registerParticipantForActivity(any(ActivityRegistrationRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/activity-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for activity"));
    }

    @Test
    void addParticipantToActivity_success() throws Exception {
        UUID activityId = UUID.randomUUID();

        AddParticipantInActivityRequestDTO request =
                new AddParticipantInActivityRequestDTO();
        request.setUserId(2L);

        ActivityRegistrationResponseDTO response =
                new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(UUID.randomUUID());
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
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for activity"));
    }

    @Test
    void getAllRegistrations_success() throws Exception {
        ActivityRegistrationDTO dto1 =
                new ActivityRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Activity 1",
                        1L,
                        "User 1",
                        CompletionStatus.NOT_COMPLETED);

        ActivityRegistrationDTO dto2 =
                new ActivityRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Activity 2",
                        2L,
                        "User 2",
                        CompletionStatus.COMPLETED);

        when(activityRegistrationService.getAllRegistrations())
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/activity-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getRegistrationById_success() throws Exception {
        UUID id = UUID.randomUUID();

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        id,
                        UUID.randomUUID(),
                        "Activity 1",
                        1L,
                        "User 1",
                        CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationById(id))
                .thenReturn(dto);

        mockMvc.perform(get("/api/activity-registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityName").value("Activity 1"));
    }

    @Test
    void getRegistrationById_notFound() throws Exception {
        when(activityRegistrationService.getRegistrationById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Registration not found"));

        mockMvc.perform(get("/api/activity-registrations/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationsByActivity_success() throws Exception {
        UUID activityId = UUID.randomUUID();

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        UUID.randomUUID(),
                        activityId,
                        "Activity 1",
                        1L,
                        "User 1",
                        CompletionStatus.NOT_COMPLETED);

        when(activityRegistrationService.getRegistrationsByActivityId(activityId))
                .thenReturn(List.of(dto));

        mockMvc.perform(
                        get("/api/activity-registrations/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRegistrationsByUser_success() throws Exception {
        Long userId = 1L;

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Activity 1",
                        userId,
                        "User 1",
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
        UUID id = UUID.randomUUID();

        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        ActivityRegistrationDTO updated =
                new ActivityRegistrationDTO(
                        id,
                        UUID.randomUUID(),
                        "Activity 1",
                        1L,
                        "User 1",
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
                .updateCompletionStatus(any(UUID.class), any()))
                .thenThrow(new ResourceNotFoundException("Registration not found"));

        ActivityCompletionUpdateDTO updateDTO = new ActivityCompletionUpdateDTO();
        updateDTO.setCompletionStatus(CompletionStatus.COMPLETED);

        mockMvc.perform(patch("/api/activity-registrations/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRegistration_success() throws Exception {
        doNothing().when(activityRegistrationService)
                .deleteRegistration(any(UUID.class));

        mockMvc.perform(delete("/api/activity-registrations/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRegistration_notFound() throws Exception {

        doThrow(new ResourceNotFoundException("Registration not found"))
                .when(activityRegistrationService)
                .deleteRegistration(any(UUID.class));

        mockMvc.perform(delete("/api/activity-registrations/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

}
