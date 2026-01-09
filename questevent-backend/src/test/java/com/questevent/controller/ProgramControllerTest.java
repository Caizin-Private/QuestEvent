package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.questevent.dto.ProgramRequestDTO;
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

    /* ===================================================
       CREATE PROGRAM
       =================================================== */

    @Test
    void createProgram_success() throws Exception {

        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setProgramTitle("Test Program");
        requestDTO.setDepartment(Department.IT);
        requestDTO.setStatus(ProgramStatus.ACTIVE);

        User host = new User();
        host.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        program.setDepartment(Department.IT);
        program.setStatus(ProgramStatus.ACTIVE);
        program.setUser(host);

        when(programService.createProgram(any(ProgramRequestDTO.class)))
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
        requestDTO.setProgramTitle("Test Program");

        when(programService.createProgram(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    /* ===================================================
       GET PROGRAMS
       =================================================== */

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
        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        program.setUser(user);

        when(programService.getProgramById(1L))
                .thenReturn(program);

        mockMvc.perform(get("/api/programs/{programId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(1L))
                .andExpect(jsonPath("$.programTitle").value("Test Program"));
    }

    @Test
    void getProgramById_notFound() throws Exception {
        when(programService.getProgramById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(get("/api/programs/{programId}", 99L))
                .andExpect(status().isNotFound());
    }

    /* ===================================================
       UPDATE PROGRAM
       =================================================== */

    @Test
    void updateProgram_success() throws Exception {
        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setProgramTitle("Updated Program");

        User user = new User();
        user.setUserId(1L);

        Program updated = new Program();
        updated.setProgramId(1L);
        updated.setProgramTitle("Updated Program");
        updated.setUser(user);

        when(programService.updateProgram(eq(1L), any(ProgramRequestDTO.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/programs/{programId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(1L))
                .andExpect(jsonPath("$.programTitle").value("Updated Program"));
    }

    @Test
    void updateProgram_notFound() throws Exception {
        when(programService.updateProgram(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(put("/api/programs/{programId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProgramRequestDTO())))
                .andExpect(status().isNotFound());
    }

    /* ===================================================
       SETTLE PROGRAM
       =================================================== */

    @Test
    void settleProgram_success() throws Exception {
        doNothing()
                .when(programWalletTransactionService)
                .manuallySettleExpiredProgramWallets(1L);

        mockMvc.perform(post("/api/programs/{programId}/settle", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Program settled successfully"));

        verify(programWalletTransactionService)
                .manuallySettleExpiredProgramWallets(1L);
    }

    /* ===================================================
       DELETE PROGRAM
       =================================================== */

    @Test
    void deleteProgram_success() throws Exception {
        doNothing().when(programService).deleteProgram(1L);

        mockMvc.perform(delete("/api/programs/{programId}", 1L))
                .andExpect(status().isNoContent());

        verify(programService).deleteProgram(1L);
    }

    @Test
    void deleteProgram_notFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"))
                .when(programService).deleteProgram(99L);

        mockMvc.perform(delete("/api/programs/{programId}", 99L))
                .andExpect(status().isNotFound());
    }
}
