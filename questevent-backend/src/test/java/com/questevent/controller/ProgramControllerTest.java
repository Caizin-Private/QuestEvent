package com.questevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.enums.Role;
import com.questevent.service.ProgramService;
import com.questevent.service.ProgramWalletTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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
        mockMvc = MockMvcBuilders
                .standaloneSetup(programController)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private User mockHost(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void createProgram_success() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();

        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setJudgeUserId(2L);
        requestDTO.setProgramTitle("Test Program");
        requestDTO.setDepartment(Department.IT);
        requestDTO.setStatus(ProgramStatus.ACTIVE);

        User host = mockHost(1L);

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");
        program.setDepartment(Department.IT);
        program.setStatus(ProgramStatus.ACTIVE);
        program.setUser(host);

        when(programService.createProgram(any()))
                .thenReturn(program);

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programId").value(programId.toString()))
                .andExpect(jsonPath("$.programTitle").value("Test Program"))
                .andExpect(jsonPath("$.hostUserId").value(1L));
    }

    @Test
    void createProgram_userNotFound() throws Exception {

        mockAuthenticatedUser(999L);

        when(programService.createProgram(any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProgramRequestDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPrograms_success() throws Exception {

        mockAuthenticatedUser(1L);

        User host = mockHost(1L);

        Program program1 = new Program();
        program1.setProgramId(UUID.randomUUID());
        program1.setUser(host);

        Program program2 = new Program();
        program2.setProgramId(UUID.randomUUID());
        program2.setUser(host);

        when(programService.getAllPrograms())
                .thenReturn(List.of(program1, program2));

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getProgramById_success() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();
        User host = mockHost(1L);

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");
        program.setUser(host);

        when(programService.getProgramById(programId))
                .thenReturn(program);

        mockMvc.perform(get("/api/programs/{programId}", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(programId.toString()));
    }

    @Test
    void getProgramById_notFound() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();

        when(programService.getProgramById(programId))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Program not found"
                ));

        mockMvc.perform(get("/api/programs/{programId}", programId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProgram_success() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();
        User host = mockHost(1L);

        Program updatedProgram = new Program();
        updatedProgram.setProgramId(programId);
        updatedProgram.setProgramTitle("Updated Program");
        updatedProgram.setUser(host);

        when(programService.updateProgram(eq(programId), any()))
                .thenReturn(updatedProgram);

        mockMvc.perform(put("/api/programs/{programId}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProgramRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(programId.toString()));
    }

    @Test
    void deleteProgram_success() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();

        doNothing().when(programService).deleteProgram(programId);

        mockMvc.perform(delete("/api/programs/{programId}", programId))
                .andExpect(status().isNoContent());

        verify(programService).deleteProgram(programId);
    }

    @Test
    void settleProgram_endpointNotPresent_shouldReturn404() throws Exception {

        mockAuthenticatedUser(1L);

        UUID programId = UUID.randomUUID();

        mockMvc.perform(post("/api/programs/{programId}/settle", programId))
                .andExpect(status().isNotFound());
    }
}
