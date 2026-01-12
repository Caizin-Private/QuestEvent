package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.questevent.dto.AddParticipantInProgramRequestDTO;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

        mockMvc = MockMvcBuilders
                .standaloneSetup(programRegistrationController)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ------------------------------------------------------------------
    // REGISTER (SELF)
    // ------------------------------------------------------------------

    @Test
    void registerParticipant_success() throws Exception {
        UUID programId = UUID.randomUUID();

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO(programId);

        ProgramRegistrationResponseDTO response =
                new ProgramRegistrationResponseDTO();
        response.setProgramRegistrationId(UUID.randomUUID());
        response.setProgramId(programId);
        response.setUserId(1L);
        response.setMessage("Successfully registered for program");

        when(programRegistrationService
                .registerParticipantForProgram(any(ProgramRegistrationRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/program-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programId").value(programId.toString()))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for program"));
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    @Test
    void getAllRegistrations_success() throws Exception {
        ProgramRegistrationDTO dto1 =
                new ProgramRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Program 1",
                        1L,
                        "User 1",
                        Instant.now());

        ProgramRegistrationDTO dto2 =
                new ProgramRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Program 2",
                        2L,
                        "User 2",
                        Instant.now());

        when(programRegistrationService.getAllRegistrations())
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/program-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getRegistrationById_success() throws Exception {
        UUID registrationId = UUID.randomUUID();

        ProgramRegistrationDTO dto =
                new ProgramRegistrationDTO(
                        registrationId,
                        UUID.randomUUID(),
                        "Program 1",
                        1L,
                        "User 1",
                        Instant.now());

        when(programRegistrationService.getRegistrationById(registrationId))
                .thenReturn(dto);

        mockMvc.perform(get("/api/program-registrations/{id}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programTitle").value("Program 1"));
    }


    @Test
    void addParticipantByHost_success() throws Exception {

        UUID programId = UUID.randomUUID();

        AddParticipantInProgramRequestDTO request =
                new AddParticipantInProgramRequestDTO();
        request.setUserId(2L);

        ProgramRegistrationResponseDTO response =
                new ProgramRegistrationResponseDTO();
        response.setProgramRegistrationId(UUID.randomUUID());
        response.setProgramId(programId);
        response.setUserId(2L);
        response.setMessage("Successfully registered for program");

        when(programRegistrationService
                .addParticipantToProgram(eq(programId), any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/program-registrations/programs/{programId}/participants",
                                programId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.message")
                        .value("Successfully registered for program"));
    }

    @Test
    void getParticipantCount_success() throws Exception {

        UUID programId = UUID.randomUUID();

        when(programRegistrationService
                .getParticipantCountForProgram(programId))
                .thenReturn(5L);

        mockMvc.perform(
                        get("/api/program-registrations/programs/{programId}/count",
                                programId))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }


    @Test
    void removeParticipantByHost_success() throws Exception {

        UUID programId = UUID.randomUUID();
        Long userId = 2L;

        doNothing().when(programRegistrationService)
                .deleteRegistrationByProgramAndUser(programId, userId);

        mockMvc.perform(
                        delete("/api/program-registrations/programs/{programId}/participants/{userId}",
                                programId, userId))
                .andExpect(status().isNoContent());

        verify(programRegistrationService)
                .deleteRegistrationByProgramAndUser(programId, userId);
    }

    @Test
    void removeParticipantByHost_notFound() throws Exception {

        UUID programId = UUID.randomUUID();
        Long userId = 99L;

        doThrow(new RuntimeException("Registration not found"))
                .when(programRegistrationService)
                .deleteRegistrationByProgramAndUser(programId, userId);

        mockMvc.perform(
                        delete("/api/program-registrations/programs/{programId}/participants/{userId}",
                                programId, userId))
                .andExpect(status().isNotFound());
    }







    @Test
    void getRegistrationById_notFound() throws Exception {
        UUID registrationId = UUID.randomUUID();

        when(programRegistrationService.getRegistrationById(registrationId))
                .thenThrow(new RuntimeException("Registration not found"));

        mockMvc.perform(get("/api/program-registrations/{id}", registrationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationsByProgram_success() throws Exception {
        UUID programId = UUID.randomUUID();

        ProgramRegistrationDTO dto =
                new ProgramRegistrationDTO(
                        UUID.randomUUID(),
                        programId,
                        "Program 1",
                        1L,
                        "User 1",
                        Instant.now());

        when(programRegistrationService.getRegistrationsByProgramId(programId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/program-registrations/programs/{programId}", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRegistrationsByUser_success() throws Exception {
        Long userId = 1L;

        ProgramRegistrationDTO dto =
                new ProgramRegistrationDTO(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Program 1",
                        userId,
                        "User 1",
                        Instant.now());

        when(programRegistrationService.getRegistrationsByUserId(userId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/program-registrations/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    @Test
    void deleteRegistration_success() throws Exception {
        UUID registrationId = UUID.randomUUID();

        doNothing().when(programRegistrationService)
                .deleteRegistration(registrationId);

        mockMvc.perform(delete("/api/program-registrations/{id}", registrationId))
                .andExpect(status().isNoContent());

        verify(programRegistrationService, times(1))
                .deleteRegistration(registrationId);
    }

    @Test
    void deleteRegistration_notFound() throws Exception {
        UUID registrationId = UUID.randomUUID();

        doThrow(new RuntimeException("Registration not found"))
                .when(programRegistrationService)
                .deleteRegistration(registrationId);

        mockMvc.perform(delete("/api/program-registrations/{id}", registrationId))
                .andExpect(status().isNotFound());
    }
}
