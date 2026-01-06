package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.ProgramResponseDTO;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.service.ProgramService;
import com.questevent.service.ProgramWalletTransactionService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgramControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramWalletTransactionService programWalletTransactionService;

    @InjectMocks
    private ProgramController programController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(programController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void createProgram_success() throws Exception {
        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setHostUserId(1L);
        requestDTO.setProgramTitle("Test Program");
        requestDTO.setDepartment(Department.IT);
        requestDTO.setRegistrationFee(100);
        requestDTO.setStatus(ProgramStatus.ACTIVE);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        program.setDepartment(Department.IT);
        program.setRegistrationFee(100);
        program.setStatus(ProgramStatus.ACTIVE);
        program.setUser(user);

        when(programService.createProgram(eq(1L), any(ProgramRequestDTO.class)))
                .thenReturn(program);

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programId").value(1L))
                .andExpect(jsonPath("$.programTitle").value("Test Program"))
                .andExpect(jsonPath("$.hostUserId").value(1L));
    }

    @Test
    void createProgram_userNotFound() throws Exception {
        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setHostUserId(999L);
        requestDTO.setProgramTitle("Test Program");

        when(programService.createProgram(eq(999L), any(ProgramRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPrograms_success() throws Exception {
        User user = new User();
        user.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Program 1");
        program1.setUser(user);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Program 2");
        program2.setUser(user);

        when(programService.getAllPrograms())
                .thenReturn(List.of(program1, program2));

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].programId").value(1L))
                .andExpect(jsonPath("$[1].programId").value(2L));
    }

    @Test
    void getProgramById_success() throws Exception {
        Long programId = 1L;
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");
        program.setUser(user);

        when(programService.getProgramById(programId))
                .thenReturn(program);

        mockMvc.perform(get("/api/programs/{programId}", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(programId))
                .andExpect(jsonPath("$.programTitle").value("Test Program"));
    }

    @Test
    void getProgramById_notFound() throws Exception {
        Long programId = 999L;

        when(programService.getProgramById(programId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(get("/api/programs/{programId}", programId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProgramsByUserId_success() throws Exception {
        Long userId = 1L;
        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Program 1");
        program.setUser(user);

        when(programService.getProgramsByUserId(userId))
                .thenReturn(List.of(program));

        mockMvc.perform(get("/api/programs/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programId").value(1L));
    }

    @Test
    void updateProgram_success() throws Exception {
        Long programId = 1L;
        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setHostUserId(1L);
        requestDTO.setProgramTitle("Updated Program");

        User user = new User();
        user.setUserId(1L);

        Program updatedProgram = new Program();
        updatedProgram.setProgramId(programId);
        updatedProgram.setProgramTitle("Updated Program");
        updatedProgram.setUser(user);

        when(programService.updateProgram(eq(1L), eq(programId), any(ProgramRequestDTO.class)))
                .thenReturn(updatedProgram);

        mockMvc.perform(put("/api/programs/{programId}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(programId))
                .andExpect(jsonPath("$.programTitle").value("Updated Program"));
    }

    @Test
    void updateProgram_notFound() throws Exception {
        Long programId = 999L;
        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setHostUserId(1L);

        when(programService.updateProgram(eq(1L), eq(programId), any(ProgramRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(put("/api/programs/{programId}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void settleProgram_success() throws Exception {
        Long programId = 1L;

        doNothing().when(programWalletTransactionService).manuallySettleExpiredProgramWallets(programId);

        mockMvc.perform(post("/api/programs/{programId}/settle", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Program settled successfully"));

        verify(programWalletTransactionService, times(1)).manuallySettleExpiredProgramWallets(programId);
    }

    @Test
    void deleteProgram_success() throws Exception {
        Long programId = 1L;
        Long userId = 1L;

        doNothing().when(programService).deleteProgram(userId, programId);

        mockMvc.perform(delete("/api/programs/{programId}", programId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        verify(programService, times(1)).deleteProgram(userId, programId);
    }

    @Test
    void deleteProgram_notFound() throws Exception {
        Long programId = 999L;
        Long userId = 1L;

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"))
                .when(programService).deleteProgram(userId, programId);

        mockMvc.perform(delete("/api/programs/{programId}", programId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }
}
