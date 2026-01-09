package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
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
    private ProgramRegistrationRepository programRegistrationRepository;

    @InjectMocks
    private ProgramService programService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @Test
    void createProgram_success() {
        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        User judgeUser = new User();
        judgeUser.setUserId(2L);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Test Program");
        dto.setStatus(ProgramStatus.DRAFT);
        dto.setJudgeUserId(2L);

        Program saved = new Program();
        saved.setProgramId(10L);
        saved.setUser(host);

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(userRepository.findById(2L)).thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(2L)).thenReturn(Optional.empty());
        when(programRepository.save(any())).thenReturn(saved);

        Program result = programService.createProgram(dto);

        assertEquals(10L, result.getProgramId());
        verify(programRepository).save(any());
    }

    @Test
    void createProgram_userNotFound() {
        mockAuthenticatedUser(1L);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    @Test
    void createProgram_creatorCannotBeJudge() {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Creator cannot be the judge", ex.getReason());
    }

    @Test
    void updateProgram_success() {
        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        Program program = new Program();
        program.setProgramId(5L);
        program.setUser(host);
        program.setProgramTitle("Old");

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(programRepository.findById(5L)).thenReturn(Optional.of(program));
        when(programRepository.save(any())).thenReturn(program);

        Program result = programService.updateProgram(5L, dto);

        assertEquals("Updated", result.getProgramTitle());
    }

    @Test
    void updateProgram_programNotFound() {
        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(programRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(99L, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Program not found", ex.getReason());
    }

    @Test
    void updateProgram_permissionDenied() {
        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        User other = new User();
        other.setUserId(2L);

        Program program = new Program();
        program.setProgramId(3L);
        program.setUser(other);

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(programRepository.findById(3L)).thenReturn(Optional.of(program));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(3L, new ProgramRequestDTO())
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You do not have permission to update this program", ex.getReason());
    }

    @Test
    void getCompletedProgramsForUser_success() {
        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program completed = new Program();
        completed.setStatus(ProgramStatus.COMPLETED);

        Program active = new Program();
        active.setStatus(ProgramStatus.ACTIVE);

        ProgramRegistration r1 = new ProgramRegistration();
        r1.setProgram(completed);

        ProgramRegistration r2 = new ProgramRegistration();
        r2.setProgram(active);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRegistrationRepository.findByUserUserId(1L))
                .thenReturn(List.of(r1, r2));

        List<Program> result =
                programService.getCompletedProgramsForUser();

        assertEquals(1, result.size());
        assertEquals(ProgramStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void getCompletedProgramsForUser_userNotFound() {
        mockAuthenticatedUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> programService.getCompletedProgramsForUser()
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }
}
