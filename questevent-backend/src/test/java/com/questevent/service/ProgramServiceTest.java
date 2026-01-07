package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.enums.Role;
import com.questevent.repository.JudgeRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

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
        dto.setStatus(ProgramStatus.ACTIVE);
        dto.setJudgeUserId(2L);

        Program savedProgram = new Program();
        savedProgram.setProgramId(1L);
        savedProgram.setProgramTitle("Test Program");
        savedProgram.setDepartment(Department.IT);
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(authUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(existingProgram));
        when(programRepository.save(any())).thenReturn(existingProgram);

        Program result = programService.updateProgram(1L, dto);

        assertEquals("New Title", result.getProgramTitle());
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
}
