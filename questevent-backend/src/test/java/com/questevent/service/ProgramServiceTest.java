package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.enums.Role;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.JudgeRepository;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    private void mockAuthenticatedUser(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
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

        Executable executable =
                () -> programService.updateProgram(programId, new ProgramRequestDTO());

        ProgramNotFoundException ex =
                assertThrows(ProgramNotFoundException.class, executable);

        assertEquals("Program not found", ex.getMessage());
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

        Executable executable =
                () -> programService.updateProgram(programId, new ProgramRequestDTO());

        AccessDeniedException ex =
                assertThrows(AccessDeniedException.class, executable);

        assertEquals(
                "You do not have permission to update this program",
                ex.getMessage()
        );
    }
}
