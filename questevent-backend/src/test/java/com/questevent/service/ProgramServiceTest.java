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
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
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

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // -------------------- CREATE PROGRAM --------------------

    @Test
    void createProgram_success() {

        Long authUserId = 1L;
        Long judgeUserId = 2L;
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

        verify(programRepository).save(any(Program.class));
    }

    @Test
    void createProgram_userNotFound() {

        Long authUserId = 1L;
        Long judgeUserId = 2L;

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
    }

    // -------------------- UPDATE PROGRAM --------------------

    @Test
    void updateProgram_success() {

        Long authUserId = 1L;
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

        Long authUserId = 1L;
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

        Long authUserId = 1L;
        Long otherUserId = 2L;
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

        Long authUserId = 1L;
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

        Long authUserId = 1L;
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

        assertDoesNotThrow(() -> programService.deleteProgram(programId));

        verify(programRepository).delete(program);
    }

    // -------------------- GET COMPLETED PROGRAMS FOR USER --------------------

    @Test
    void getCompletedProgramsForUser_success() {

        Long authUserId = 1L;

        mockAuthenticatedUser(authUserId);

        User authUser = new User();
        authUser.setUserId(authUserId);

        Program completedProgram = new Program();
        completedProgram.setStatus(ProgramStatus.COMPLETED);

        Program activeProgram = new Program();
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
}
