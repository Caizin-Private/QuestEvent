package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.questevent.dto.*;
import com.questevent.enums.CompletionStatus;
import com.questevent.service.ActivityRegistrationService;
import jakarta.servlet.ServletException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        when(activityRegistrationService.registerParticipantForActivity(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/activity-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void addParticipantToActivity_success() throws Exception {

        UUID activityId = UUID.randomUUID();

        AddParticipantInActivityRequestDTO request = new AddParticipantInActivityRequestDTO();
        request.setUserId(2L);

        ActivityRegistrationResponseDTO response = new ActivityRegistrationResponseDTO();
        response.setActivityRegistrationId(UUID.randomUUID());
        response.setActivityId(activityId);
        response.setUserId(2L);

        when(activityRegistrationService.addParticipantToActivity(eq(activityId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/activity-registrations/activities/{activityId}/participants", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    void getAllRegistrations_success() throws Exception {

        when(activityRegistrationService.getAllRegistrations())
                .thenReturn(List.of(mock(ActivityRegistrationDTO.class)));

        mockMvc.perform(get("/api/activity-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRegistrationById_success() throws Exception {

        UUID id = UUID.randomUUID();

        ActivityRegistrationDTO dto =
                new ActivityRegistrationDTO(
                        id,
                        UUID.randomUUID(),
                        "Activity",
                        1L,
                        "User",
                        CompletionStatus.NOT_COMPLETED
                );

        when(activityRegistrationService.getRegistrationById(id))
                .thenReturn(dto);

        mockMvc.perform(get("/api/activity-registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityName").value("Activity"));
    }

    @Test
    void getRegistrationById_exception_propagates() {

        when(activityRegistrationService.getRegistrationById(any()))
                .thenThrow(new RuntimeException("Not found"));

        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(
                        get("/api/activity-registrations/{id}", UUID.randomUUID())
                ).andReturn()
        );

        Throwable root = exception.getCause() != null ? exception.getCause() : exception;
        assertTrue(root instanceof RuntimeException);
    }


    @Test
    void deleteRegistration_success() throws Exception {

        doNothing().when(activityRegistrationService)
                .deleteRegistration(any());

        mockMvc.perform(delete("/api/activity-registrations/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRegistration_exception_propagates() {

        doThrow(new RuntimeException("Not found"))
                .when(activityRegistrationService)
                .deleteRegistration(any());

        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(
                        delete("/api/activity-registrations/{id}", UUID.randomUUID())
                ).andReturn()
        );

        Throwable root = exception.getCause() != null ? exception.getCause() : exception;
        assertTrue(root instanceof RuntimeException);
    }

}
