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
import com.questevent.dto.UserPrincipal;
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------- SECURITY MOCK --------------------

    private void mockAuthenticatedUser(Long userId) {
        User user = new User();
        user.setUserId(userId);

        UserPrincipal principal = new UserPrincipal(
                userId,
                "test@questevent.com",
                Role.USER
        );


        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // -------------------- CREATE PROGRAM --------------------

    @Test
    void createProgram_success() throws Exception {
        mockAuthenticatedUser(1L);

        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setJudgeUserId(2L);
        requestDTO.setProgramTitle("Test Program");
        requestDTO.setDepartment(Department.IT);
        requestDTO.setRegistrationFee(100);
        requestDTO.setStatus(ProgramStatus.ACTIVE);

        User host = new User();
        host.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        program.setDepartment(Department.IT);
        program.setRegistrationFee(100);
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
        mockAuthenticatedUser(999L);

        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setJudgeUserId(2L);
        requestDTO.setProgramTitle("Test Program");

        when(programService.createProgram(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    // -------------------- GET ALL PROGRAMS --------------------

    @Test
    void getAllPrograms_success() throws Exception {
        mockAuthenticatedUser(1L);

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

    // -------------------- GET PROGRAM BY ID --------------------

    @Test
    void getProgramById_success() throws Exception {
        mockAuthenticatedUser(1L);

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
        mockAuthenticatedUser(1L);

        when(programService.getProgramById(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(get("/api/programs/{programId}", 999L))
                .andExpect(status().isNotFound());
    }

    // -------------------- GET MY PROGRAMS --------------------

    @Test
    void getMyPrograms_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("My Program");
        program.setUser(user);

        when(programService.getMyPrograms())
                .thenReturn(List.of(program));

        mockMvc.perform(get("/api/programs/my-programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programId").value(1L));
    }



    // -------------------- UPDATE PROGRAM --------------------

    @Test
    void updateProgram_success() throws Exception {
        mockAuthenticatedUser(1L);

        ProgramRequestDTO requestDTO = new ProgramRequestDTO();
        requestDTO.setProgramTitle("Updated Program");

        User user = new User();
        user.setUserId(1L);

        Program updatedProgram = new Program();
        updatedProgram.setProgramId(1L);
        updatedProgram.setProgramTitle("Updated Program");
        updatedProgram.setUser(user);

        when(programService.updateProgram(eq(1L), any(ProgramRequestDTO.class)))
                .thenReturn(updatedProgram);

        mockMvc.perform(put("/api/programs/{programId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programTitle").value("Updated Program"));
    }

    @Test
    void updateProgram_notFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.updateProgram(eq(999L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(put("/api/programs/{programId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProgramRequestDTO())))
                .andExpect(status().isNotFound());
    }

    // -------------------- SETTLE PROGRAM --------------------

    @Test
    void settleProgram_success() throws Exception {
        mockAuthenticatedUser(1L);

        doNothing().when(programWalletTransactionService)
                .manuallySettleExpiredProgramWallets(1L);

        mockMvc.perform(post("/api/programs/{programId}/settle", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Program settled successfully"));

        verify(programWalletTransactionService, times(1))
                .manuallySettleExpiredProgramWallets(1L);
    }

    // -------------------- DELETE PROGRAM --------------------

    @Test
    void deleteProgram_success() throws Exception {
        mockAuthenticatedUser(1L);

        doNothing().when(programService).deleteProgram(1L);

        mockMvc.perform(delete("/api/programs/{programId}", 1L))
                .andExpect(status().isNoContent());

        verify(programService, times(1)).deleteProgram(1L);
    }

    @Test
    void deleteProgram_notFound() throws Exception {
        mockAuthenticatedUser(1L);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"))
                .when(programService).deleteProgram(999L);

        mockMvc.perform(delete("/api/programs/{programId}", 999L))
                .andExpect(status().isNotFound());
    }


    // -------------------- GET COMPLETED PROGRAMS FOR USER --------------------

    @Test
    void getCompletedProgramsForUser_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Completed Program");
        program.setStatus(ProgramStatus.COMPLETED);
        program.setUser(user);

        when(programService.getCompletedProgramsForUser())
                .thenReturn(List.of(program));

        mockMvc.perform(get("/api/programs/my-completed-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programId").value(1L))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getCompletedProgramsForUser_userNotFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.getCompletedProgramsForUser())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/programs/my-completed-registrations"))
                .andExpect(status().isNotFound());
    }

    // -------------------- GET PROGRAMS WHERE USER IS JUDGE --------------------

    @Test
    void getProgramsWhereUserIsJudge_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Judge Program 1");
        program1.setUser(user);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Judge Program 2");
        program2.setUser(user);

        when(programService.getProgramsWhereUserIsJudge())
                .thenReturn(List.of(program1, program2));

        mockMvc.perform(get("/api/programs/my-judge-programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].programId").value(1L))
                .andExpect(jsonPath("$[1].programId").value(2L));
    }

    @Test
    void getProgramsWhereUserIsJudge_userNotFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.getProgramsWhereUserIsJudge())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/programs/my-judge-programs"))
                .andExpect(status().isNotFound());
    }

    // -------------------- GET ACTIVE PROGRAMS BY DEPARTMENT --------------------

    @Test
    void getActiveProgramsByUserDepartment_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Active Program 1");
        program1.setStatus(ProgramStatus.ACTIVE);
        program1.setDepartment(Department.IT);
        program1.setUser(user);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Active Program 2");
        program2.setStatus(ProgramStatus.ACTIVE);
        program2.setDepartment(Department.IT);
        program2.setUser(user);

        when(programService.getActiveProgramsByUserDepartment())
                .thenReturn(List.of(program1, program2));

        mockMvc.perform(get("/api/programs/active-by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"));
    }

    @Test
    void getActiveProgramsByUserDepartment_userNotFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.getActiveProgramsByUserDepartment())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/programs/active-by-department"))
                .andExpect(status().isNotFound());
    }

    // -------------------- GET DRAFT PROGRAMS BY HOST --------------------

    @Test
    void getDraftProgramsByHost_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Draft Program 1");
        program1.setStatus(ProgramStatus.DRAFT);
        program1.setUser(user);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Draft Program 2");
        program2.setStatus(ProgramStatus.DRAFT);
        program2.setUser(user);

        when(programService.getDraftProgramsByHost())
                .thenReturn(List.of(program1, program2));

        mockMvc.perform(get("/api/programs/my-draft-programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].status").value("DRAFT"));
    }

    @Test
    void getDraftProgramsByHost_userNotFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.getDraftProgramsByHost())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/programs/my-draft-programs"))
                .andExpect(status().isNotFound());
    }

    // -------------------- CHANGE PROGRAM STATUS TO DRAFT --------------------

    @Test
    void changeProgramStatusToDraft_success() throws Exception {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        program.setStatus(ProgramStatus.DRAFT);
        program.setUser(user);

        when(programService.changeProgramStatusToActive(1L))
                .thenReturn(program);

        mockMvc.perform(patch("/api/programs/{programId}/status-to-active", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programId").value(1L))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(programService, times(1)).changeProgramStatusToActive(1L);
    }

    @Test
    void changeProgramStatusToDraft_programNotFound() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.changeProgramStatusToActive(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        mockMvc.perform(patch("/api/programs/{programId}/status-to-active", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeProgramStatusToDraft_permissionDenied() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.changeProgramStatusToActive(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to change status of this program"));

        mockMvc.perform(patch("/api/programs/{programId}/status-to-active", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeProgramStatusToDraft_statusNotActive() throws Exception {
        mockAuthenticatedUser(1L);

        when(programService.changeProgramStatusToActive(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program status must be DRAFT to change to ACTIVE"));

        mockMvc.perform(patch("/api/programs/{programId}/status-to-active", 1L))
                .andExpect(status().isBadRequest());
    }
}
