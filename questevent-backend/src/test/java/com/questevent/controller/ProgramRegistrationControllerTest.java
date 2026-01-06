package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.service.ProgramRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgramRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProgramRegistrationService programRegistrationService;

    @InjectMocks
    private ProgramRegistrationController programRegistrationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(programRegistrationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void registerParticipant_success() throws Exception {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);
        request.setUserId(1L);

        ProgramRegistrationResponseDTO response = new ProgramRegistrationResponseDTO();
        response.setProgramRegistrationId(1L);
        response.setProgramId(1L);
        response.setUserId(1L);
        response.setMessage("Successfully registered for program");

        when(programRegistrationService.registerParticipantForProgram(any(ProgramRegistrationRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/program-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programRegistrationId").value(1L))
                .andExpect(jsonPath("$.message").value("Successfully registered for program"));
    }

    @Test
    void getAllRegistrations_success() throws Exception {
        ProgramRegistrationDTO dto1 = new ProgramRegistrationDTO(1L, 1L, "Program 1", 1L, "User 1", Instant.now());
        ProgramRegistrationDTO dto2 = new ProgramRegistrationDTO(2L, 2L, "Program 2", 2L, "User 2", Instant.now());

        when(programRegistrationService.getAllRegistrations())
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/program-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].programRegistrationId").value(1L))
                .andExpect(jsonPath("$[1].programRegistrationId").value(2L));
    }

    @Test
    void getRegistrationById_success() throws Exception {
        Long id = 1L;
        ProgramRegistrationDTO dto = new ProgramRegistrationDTO(id, 1L, "Program 1", 1L, "User 1", Instant.now());

        when(programRegistrationService.getRegistrationById(id))
                .thenReturn(dto);

        mockMvc.perform(get("/api/program-registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programRegistrationId").value(id))
                .andExpect(jsonPath("$.programTitle").value("Program 1"));
    }

    @Test
    void getRegistrationById_notFound() throws Exception {
        Long id = 999L;

        when(programRegistrationService.getRegistrationById(id))
                .thenThrow(new RuntimeException("Registration not found"));

        mockMvc.perform(get("/api/program-registrations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationsByProgram_success() throws Exception {
        Long programId = 1L;
        ProgramRegistrationDTO dto = new ProgramRegistrationDTO(1L, programId, "Program 1", 1L, "User 1", Instant.now());

        when(programRegistrationService.getRegistrationsByProgramId(programId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/program-registrations/programs/{programId}", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programId").value(programId));
    }

    @Test
    void getRegistrationsByUser_success() throws Exception {
        Long userId = 1L;
        ProgramRegistrationDTO dto = new ProgramRegistrationDTO(1L, 1L, "Program 1", userId, "User 1", Instant.now());

        when(programRegistrationService.getRegistrationsByUserId(userId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/program-registrations/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getParticipantCount_success() throws Exception {
        Long programId = 1L;
        long count = 10L;

        when(programRegistrationService.getParticipantCountForProgram(programId))
                .thenReturn(count);

        mockMvc.perform(get("/api/program-registrations/programs/{programId}/count", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(count));
    }

    @Test
    void deleteRegistration_success() throws Exception {
        Long id = 1L;

        doNothing().when(programRegistrationService).deleteRegistration(id);

        mockMvc.perform(delete("/api/program-registrations/{id}", id))
                .andExpect(status().isNoContent());

        verify(programRegistrationService, times(1)).deleteRegistration(id);
    }

    @Test
    void deleteRegistration_notFound() throws Exception {
        Long id = 999L;

        doThrow(new RuntimeException("Registration not found"))
                .when(programRegistrationService).deleteRegistration(id);

        mockMvc.perform(delete("/api/program-registrations/{id}", id))
                .andExpect(status().isNotFound());
    }
}
