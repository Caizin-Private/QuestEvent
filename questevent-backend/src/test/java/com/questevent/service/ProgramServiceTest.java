package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.enums.Role;
import com.questevent.repository.JudgeRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramRegistrationRepository programRegistrationRepository;

    @Mock
    private JudgeRepository judgeRepository;

    @Mock
    private ProgramWalletService programWalletService;

    @InjectMocks
    private ProgramService programService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------- SECURITY MOCK --------------------

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

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
    void createProgram_success() {
        mockAuthenticatedUser(1L);

        User creator = new User();
        creator.setUserId(1L);

        User judgeUser = new User();
        judgeUser.setUserId(2L);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Test Program");
        dto.setProgramDescription("Test Description");
        dto.setDepartment(Department.IT);
        dto.setRegistrationFee(100);
        dto.setStatus(ProgramStatus.ACTIVE);
        dto.setJudgeUserId(2L);

        Program savedProgram = new Program();
        savedProgram.setProgramId(1L);
        savedProgram.setProgramTitle("Test Program");
        savedProgram.setDepartment(Department.IT);
        savedProgram.setRegistrationFee(100);
        savedProgram.setStatus(ProgramStatus.ACTIVE);
        savedProgram.setUser(creator);

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(programRepository.save(any(Program.class))).thenReturn(savedProgram);

        Program result = programService.createProgram(dto);

        assertNotNull(result);
        assertEquals("Test Program", result.getProgramTitle());
        assertEquals(creator, result.getUser());

        verify(programRepository, times(1)).save(any(Program.class));
    }

    @Test
    void createProgram_userNotFound() {
        mockAuthenticatedUser(999L);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(2L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());

        verify(programRepository, never()).save(any());
    }

    // -------------------- UPDATE PROGRAM --------------------

    @Test
    void updateProgram_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program existingProgram = new Program();
        existingProgram.setProgramId(1L);
        existingProgram.setProgramTitle("Old Title");
        existingProgram.setUser(authUser);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("New Title");
        dto.setRegistrationFee(200);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(programRepository.save(any())).thenReturn(existingProgram);

        Program result = programService.updateProgram(1L, dto);

        assertEquals("New Title", result.getProgramTitle());
        assertEquals(200, result.getRegistrationFee());
    }

    @Test
    void updateProgram_programNotFound() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(999L, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void updateProgram_permissionDenied() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        User otherUser = new User();
        otherUser.setUserId(2L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setUser(otherUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(1L, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to update this program", ex.getReason());
    }

    // -------------------- GET MY PROGRAMS --------------------

    @Test
    void getMyPrograms_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findByUser_UserId(1L))
                .thenReturn(List.of(program));

        List<Program> result = programService.getMyPrograms();

        assertEquals(1, result.size());
    }

    // -------------------- GET PROGRAM BY ID --------------------

    @Test
    void getProgramById_success() {
        Program program = new Program();
        program.setProgramId(1L);

        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        Program result = programService.getProgramById(1L);

        assertNotNull(result);
    }

    // -------------------- DELETE PROGRAM --------------------

    @Test
    void deleteProgram_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setUser(authUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        assertDoesNotThrow(() -> programService.deleteProgram(1L));
        verify(programRepository).delete(program);
    }

    @Test
    void deleteProgram_permissionDenied() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        User otherUser = new User();
        otherUser.setUserId(2L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setUser(otherUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.deleteProgram(1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to delete this program", ex.getReason());
    }

    // -------------------- GET COMPLETED PROGRAMS FOR USER --------------------

    @Test
    void getCompletedProgramsForUser_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program completedProgram = new Program();
        completedProgram.setProgramId(1L);
        completedProgram.setStatus(ProgramStatus.COMPLETED);

        Program activeProgram = new Program();
        activeProgram.setProgramId(2L);
        activeProgram.setStatus(ProgramStatus.ACTIVE);

        ProgramRegistration reg1 = new ProgramRegistration();
        reg1.setProgram(completedProgram);

        ProgramRegistration reg2 = new ProgramRegistration();
        reg2.setProgram(activeProgram);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRegistrationRepository.findByUserUserId(1L))
                .thenReturn(List.of(reg1, reg2));

        List<Program> result = programService.getCompletedProgramsForUser();

        assertEquals(1, result.size());
        assertEquals(ProgramStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void getCompletedProgramsForUser_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getCompletedProgramsForUser()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    // -------------------- GET PROGRAMS WHERE USER IS JUDGE --------------------

    @Test
    void getProgramsWhereUserIsJudge_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Program 1");

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Program 2");

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findByJudgeUserId(1L))
                .thenReturn(List.of(program1, program2));

        List<Program> result = programService.getProgramsWhereUserIsJudge();

        assertEquals(2, result.size());
        verify(programRepository, times(1)).findByJudgeUserId(1L);
    }

    @Test
    void getProgramsWhereUserIsJudge_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getProgramsWhereUserIsJudge()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    // -------------------- GET ACTIVE PROGRAMS BY USER DEPARTMENT --------------------

    @Test
    void getActiveProgramsByUserDepartment_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);
        authUser.setDepartment(Department.IT);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setDepartment(Department.IT);
        program1.setStatus(ProgramStatus.ACTIVE);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setDepartment(Department.IT);
        program2.setStatus(ProgramStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findByStatusAndDepartment(ProgramStatus.ACTIVE, Department.IT))
                .thenReturn(List.of(program1, program2));

        List<Program> result = programService.getActiveProgramsByUserDepartment();

        assertEquals(2, result.size());
        result.forEach(program -> {
            assertEquals(ProgramStatus.ACTIVE, program.getStatus());
            assertEquals(Department.IT, program.getDepartment());
        });
    }

    @Test
    void getActiveProgramsByUserDepartment_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getActiveProgramsByUserDepartment()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    // -------------------- GET DRAFT PROGRAMS BY HOST --------------------

    @Test
    void getDraftProgramsByHost_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setStatus(ProgramStatus.DRAFT);
        program1.setUser(authUser);

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setStatus(ProgramStatus.DRAFT);
        program2.setUser(authUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findByStatusAndUser_UserId(ProgramStatus.DRAFT, 1L))
                .thenReturn(List.of(program1, program2));

        List<Program> result = programService.getDraftProgramsByHost();

        assertEquals(2, result.size());
        result.forEach(program -> {
            assertEquals(ProgramStatus.DRAFT, program.getStatus());
            assertEquals(1L, program.getUser().getUserId());
        });
    }

    @Test
    void getDraftProgramsByHost_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getDraftProgramsByHost()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    // -------------------- CHANGE PROGRAM STATUS TO DRAFT --------------------

    @Test
    void changeProgramStatusToDraft_success() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setStatus(ProgramStatus.DRAFT);
        program.setUser(authUser);

        Program savedProgram = new Program();
        savedProgram.setProgramId(1L);
        savedProgram.setStatus(ProgramStatus.ACTIVE);
        savedProgram.setUser(authUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRepository.save(program)).thenReturn(savedProgram);

        Program result = programService.changeProgramStatusToActive(1L);

        assertEquals(ProgramStatus.ACTIVE, result.getStatus());
        verify(programRepository, times(1)).save(program);
    }

    @Test
    void changeProgramStatusToDraft_programNotFound() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void changeProgramStatusToDraft_permissionDenied() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        User otherUser = new User();
        otherUser.setUserId(2L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setStatus(ProgramStatus.ACTIVE);
        program.setUser(otherUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to change status of this program", ex.getReason());
    }

    @Test
    void changeProgramStatusToDraft_statusNotActive() {
        mockAuthenticatedUser(1L);

        User authUser = new User();
        authUser.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);
        program.setStatus(ProgramStatus.COMPLETED);
        program.setUser(authUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(1L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Program status must be DRAFT to change to ACTIVE", ex.getReason());
    }
}
