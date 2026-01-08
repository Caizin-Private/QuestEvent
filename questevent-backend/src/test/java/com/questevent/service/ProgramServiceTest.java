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
import java.util.UUID;

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

    private void mockAuthenticatedUser(UUID userId) {
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

        UUID authUserId = UUID.randomUUID();
        UUID judgeUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User creator = new User();
        creator.setUserId(authUserId);

        User judgeUser = new User();
        judgeUser.setUserId(judgeUserId);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Test Program");
        dto.setProgramDescription("Test Description");
        dto.setDepartment(Department.IT);
        dto.setStatus(ProgramStatus.ACTIVE);
        dto.setJudgeUserId(judgeUserId);

        Program savedProgram = new Program();
        savedProgram.setProgramId(programId);
        savedProgram.setProgramTitle("Test Program");
        savedProgram.setDepartment(Department.IT);
        savedProgram.setStatus(ProgramStatus.ACTIVE);
        savedProgram.setUser(creator);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(creator));
        when(userRepository.findById(judgeUserId))
                .thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(judgeUserId))
                .thenReturn(Optional.empty());
        when(programRepository.save(any(Program.class)))
                .thenReturn(savedProgram);

        Program result = programService.createProgram(dto);

        assertNotNull(result);
        assertEquals("Test Program", result.getProgramTitle());
        assertEquals(creator, result.getUser());

        verify(programRepository, times(1)).save(any(Program.class));
    }

    @Test
    void createProgram_userNotFound() {

        UUID authUserId = UUID.randomUUID();
        UUID judgeUserId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(judgeUserId);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

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

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program existingProgram = new Program();
        existingProgram.setProgramId(programId);
        existingProgram.setProgramTitle("Old Title");
        existingProgram.setUser(authUser);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("New Title");

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(existingProgram));
        when(programRepository.save(any()))
                .thenReturn(existingProgram);

        Program result =
                programService.updateProgram(programId, dto);

        assertEquals("New Title", result.getProgramTitle());
    }

    @Test
    void updateProgram_programNotFound() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(programId, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void updateProgram_permissionDenied() {

        UUID authUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        User otherUser = new User();
        otherUser.setUserId(otherUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(otherUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(programId, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to update this program", ex.getReason());
    }

    // -------------------- GET MY PROGRAMS --------------------

    @Test
    void getMyPrograms_success() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program = new Program();
        program.setProgramId(programId);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findByUser_UserId(authUserId))
                .thenReturn(List.of(program));

        List<Program> result = programService.getMyPrograms();

        assertEquals(1, result.size());
    }

    // -------------------- GET PROGRAM BY ID --------------------

    @Test
    void getProgramById_success() {

        UUID programId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        Program result = programService.getProgramById(programId);

        assertNotNull(result);
    }

    // -------------------- DELETE PROGRAM --------------------

    @Test
    void deleteProgram_success() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(authUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertDoesNotThrow(() ->
                programService.deleteProgram(programId));

        verify(programRepository).delete(program);
    }

    @Test
    void deleteProgram_permissionDenied() {

        UUID authUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        User otherUser = new User();
        otherUser.setUserId(otherUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(otherUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.deleteProgram(programId)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to delete this program", ex.getReason());
    }

    // -------------------- GET COMPLETED PROGRAMS FOR USER --------------------

    @Test
    void getCompletedProgramsForUser_success() {

        UUID authUserId = UUID.randomUUID();
        UUID completedProgramId = UUID.randomUUID();
        UUID activeProgramId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program completedProgram = new Program();
        completedProgram.setProgramId(completedProgramId);
        completedProgram.setStatus(ProgramStatus.COMPLETED);

        Program activeProgram = new Program();
        activeProgram.setProgramId(activeProgramId);
        activeProgram.setStatus(ProgramStatus.ACTIVE);

        ProgramRegistration reg1 = new ProgramRegistration();
        reg1.setProgram(completedProgram);

        ProgramRegistration reg2 = new ProgramRegistration();
        reg2.setProgram(activeProgram);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRegistrationRepository.findByUserUserId(authUserId))
                .thenReturn(List.of(reg1, reg2));

        List<Program> result =
                programService.getCompletedProgramsForUser();

        assertEquals(1, result.size());
        assertEquals(ProgramStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void getCompletedProgramsForUser_userNotFound() {

        UUID authUserId = UUID.randomUUID();

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

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

        UUID authUserId = UUID.randomUUID();
        UUID programId1 = UUID.randomUUID();
        UUID programId2 = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program1 = new Program();
        program1.setProgramId(programId1);
        program1.setProgramTitle("Program 1");

        Program program2 = new Program();
        program2.setProgramId(programId2);
        program2.setProgramTitle("Program 2");

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findByJudgeUserId(authUserId))
                .thenReturn(List.of(program1, program2));

        List<Program> result =
                programService.getProgramsWhereUserIsJudge();

        assertEquals(2, result.size());
        verify(programRepository, times(1))
                .findByJudgeUserId(authUserId);
    }

    @Test
    void getProgramsWhereUserIsJudge_userNotFound() {

        UUID authUserId = UUID.randomUUID();

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

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

        UUID authUserId = UUID.randomUUID();
        UUID programId1 = UUID.randomUUID();
        UUID programId2 = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);
        authUser.setDepartment(Department.IT);

        Program program1 = new Program();
        program1.setProgramId(programId1);
        program1.setDepartment(Department.IT);
        program1.setStatus(ProgramStatus.ACTIVE);

        Program program2 = new Program();
        program2.setProgramId(programId2);
        program2.setDepartment(Department.IT);
        program2.setStatus(ProgramStatus.ACTIVE);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE, Department.IT))
                .thenReturn(List.of(program1, program2));

        List<Program> result =
                programService.getActiveProgramsByUserDepartment();

        assertEquals(2, result.size());
        result.forEach(program -> {
            assertEquals(ProgramStatus.ACTIVE, program.getStatus());
            assertEquals(Department.IT, program.getDepartment());
        });
    }

    @Test
    void getActiveProgramsByUserDepartment_userNotFound() {

        UUID authUserId = UUID.randomUUID();

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

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

        UUID authUserId = UUID.randomUUID();
        UUID programId1 = UUID.randomUUID();
        UUID programId2 = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program1 = new Program();
        program1.setProgramId(programId1);
        program1.setStatus(ProgramStatus.DRAFT);
        program1.setUser(authUser);

        Program program2 = new Program();
        program2.setProgramId(programId2);
        program2.setStatus(ProgramStatus.DRAFT);
        program2.setUser(authUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findByStatusAndUser_UserId(
                ProgramStatus.DRAFT, authUserId))
                .thenReturn(List.of(program1, program2));

        List<Program> result =
                programService.getDraftProgramsByHost();

        assertEquals(2, result.size());
        result.forEach(program -> {
            assertEquals(ProgramStatus.DRAFT, program.getStatus());
            assertEquals(authUserId, program.getUser().getUserId());
        });
    }

    @Test
    void getDraftProgramsByHost_userNotFound() {

        UUID authUserId = UUID.randomUUID();

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getDraftProgramsByHost()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    // -------------------- CHANGE PROGRAM STATUS TO ACTIVE --------------------

    @Test
    void changeProgramStatusToDraft_success() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setStatus(ProgramStatus.DRAFT);
        program.setUser(authUser);

        Program savedProgram = new Program();
        savedProgram.setProgramId(programId);
        savedProgram.setStatus(ProgramStatus.ACTIVE);
        savedProgram.setUser(authUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.save(program))
                .thenReturn(savedProgram);

        Program result =
                programService.changeProgramStatusToActive(programId);

        assertEquals(ProgramStatus.ACTIVE, result.getStatus());
        verify(programRepository, times(1)).save(program);
    }

    @Test
    void changeProgramStatusToDraft_programNotFound() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(programId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void changeProgramStatusToDraft_permissionDenied() {

        UUID authUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        User otherUser = new User();
        otherUser.setUserId(otherUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setStatus(ProgramStatus.ACTIVE);
        program.setUser(otherUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(programId)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(
                "You do not have permission to change status of this program",
                ex.getReason()
        );
    }

    @Test
    void changeProgramStatusToDraft_statusNotActive() {

        UUID authUserId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setStatus(ProgramStatus.COMPLETED);
        program.setUser(authUser);

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.of(authUser));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.changeProgramStatusToActive(programId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(
                "Program status must be DRAFT to change to ACTIVE",
                ex.getReason()
        );
    }
}
