package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.enums.Role;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UnauthorizedException;
import com.questevent.exception.UserNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Executable;
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
    }

    @Test
    void createProgram_userNotFound() {

        Long authUserId = 1L;
        mockAuthenticatedUser(authUserId);

        ProgramRequestDTO dto = new ProgramRequestDTO();

        when(userRepository.findById(authUserId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals("User not found", ex.getMessage());
    }

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

        ProgramRequestDTO dto = new ProgramRequestDTO();

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> programService.updateProgram(programId, dto)
        );

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

        ProgramRequestDTO dto = new ProgramRequestDTO();

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> programService.updateProgram(programId, dto)
        );

        assertEquals(
                "You do not have permission to update this program",
                ex.getMessage()
        );
    }

    @Test
    void createProgram_shouldThrowUnauthorized_whenNoAuthentication() {

        SecurityContextHolder.clearContext();

        ProgramRequestDTO dto = new ProgramRequestDTO();

        assertThrows(
                UnauthorizedException.class,
                () -> programService.createProgram(dto)
        );
    }

    @Test
    void createProgram_shouldThrowJudgeUserNotFound() {

        Long hostId = 1L;
        Long judgeId = 2L;

        mockAuthenticatedUser(hostId);

        User host = new User();
        host.setUserId(hostId);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(judgeId);

        when(userRepository.findById(hostId))
                .thenReturn(Optional.of(host));
        when(userRepository.findById(judgeId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals("Judge user not found", ex.getMessage());
    }

    @Test
    void createProgram_shouldThrow_whenHostAssignsSelfAsJudge() {

        Long userId = 1L;
        mockAuthenticatedUser(userId);

        User user = new User();
        user.setUserId(userId);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals("Creator cannot be the judge", ex.getMessage());
    }

    @Test
    void updateProgram_shouldThrowUnauthorized_whenNoAuth() {

        SecurityContextHolder.clearContext();

        UUID programId = UUID.randomUUID();
        ProgramRequestDTO dto = new ProgramRequestDTO();

        assertThrows(
                UnauthorizedException.class,
                () -> programService.updateProgram(programId, dto)
        );
    }

    @Test
    void updateProgram_shouldUpdateJudgeSuccessfully() {

        Long hostId = 1L;
        Long judgeId = 2L;
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(hostId);

        User host = new User();
        host.setUserId(hostId);

        User judgeUser = new User();
        judgeUser.setUserId(judgeId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(host);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(judgeId);

        when(userRepository.findById(hostId))
                .thenReturn(Optional.of(host));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(userRepository.findById(judgeId))
                .thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(judgeId))
                .thenReturn(Optional.empty());
        when(programRepository.save(any()))
                .thenReturn(program);

        Program updated =
                programService.updateProgram(programId, dto);

        assertNotNull(updated.getJudge());
    }

    @Test
    void getMyPrograms_shouldThrowUserNotFound_whenNoAuth() {

        SecurityContextHolder.clearContext();

        assertThrows(
                UserNotFoundException.class,
                () -> programService.getMyPrograms()
        );
    }

    @Test
    void deleteProgram_shouldThrowUserNotFound_whenNoAuth() {

        SecurityContextHolder.clearContext();

        UUID programId = UUID.randomUUID();

        assertThrows(
                UserNotFoundException.class,
                () -> programService.deleteProgram(programId)
        );
    }


    @Test
    void getProgramsWhereUserIsJudge_success() {

        Long userId = 1L;
        mockAuthenticatedUser(userId);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findByJudgeUserId(userId))
                .thenReturn(List.of(new Program()));

        List<Program> result =
                programService.getProgramsWhereUserIsJudge();

        assertEquals(1, result.size());
    }

    @Test
    void getActiveProgramsByUserDepartment_success() {

        Long userId = 1L;
        mockAuthenticatedUser(userId);

        User user = new User();
        user.setUserId(userId);
        user.setDepartment(Department.IT);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE, Department.IT))
                .thenReturn(List.of(new Program()));

        List<Program> result =
                programService.getActiveProgramsByUserDepartment();

        assertEquals(1, result.size());
    }

    @Test
    void changeProgramStatusToActive_success() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(user);
        program.setStatus(ProgramStatus.DRAFT);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.save(program))
                .thenReturn(program);

        Program updated =
                programService.changeProgramStatusToActive(programId);

        assertEquals(ProgramStatus.ACTIVE, updated.getStatus());
    }





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
        when(programRepository.findByUserUserId(authUserId))
                .thenReturn(List.of(program));

        List<Program> result = programService.getMyPrograms();

        assertEquals(1, result.size());
    }

    @Test
    void createProgram_invalidAuth_shouldThrowUnauthorized() {

        Authentication auth =
                new UsernamePasswordAuthenticationToken("invalid", null);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        ProgramRequestDTO dto = new ProgramRequestDTO();

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> programService.createProgram(dto)
        );

        assertEquals("Unauthorized", ex.getMessage());
    }


    @Test
    void getMyPrograms_authenticatedUserNotInDb_shouldThrowUserNotFound() {

        mockAuthenticatedUser(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> programService.getMyPrograms()
        );
    }

    @Test
    void createProgram_existingJudgeUsed() {

        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        User judgeUser = new User();
        judgeUser.setUserId(2L);

        Judge existingJudge = new Judge();
        existingJudge.setUser(judgeUser);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(2L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(host));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(2L))
                .thenReturn(Optional.of(existingJudge));
        when(programRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Program result = programService.createProgram(dto);

        assertSame(existingJudge, result.getJudge());
    }

    @Test
    void updateProgram_judgeNotProvided_shouldKeepExistingJudge() {

        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        Judge judge = new Judge();

        Program program = new Program();
        program.setUser(host);
        program.setJudge(judge);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(host));
        when(programRepository.findById(any()))
                .thenReturn(Optional.of(program));
        when(programRepository.save(any()))
                .thenReturn(program);

        Program updated =
                programService.updateProgram(UUID.randomUUID(), new ProgramRequestDTO());

        assertSame(judge, updated.getJudge());
    }

    @Test
    void updateProgram_existingJudgeReused() {

        mockAuthenticatedUser(1L);

        User host = new User();
        host.setUserId(1L);

        User judgeUser = new User();
        judgeUser.setUserId(2L);

        Judge existingJudge = new Judge();
        existingJudge.setUser(judgeUser);

        Program program = new Program();
        program.setUser(host);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(programRepository.findById(any())).thenReturn(Optional.of(program));
        when(userRepository.findById(2L)).thenReturn(Optional.of(judgeUser));
        when(judgeRepository.findByUserUserId(2L))
                .thenReturn(Optional.of(existingJudge));
        when(programRepository.save(any())).thenReturn(program);

        Program updated =
                programService.updateProgram(UUID.randomUUID(), dto);

        assertSame(existingJudge, updated.getJudge());
    }






    @Test
    void getDraftProgramsByHost_success() {

        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(programRepository.findByStatusAndUserUserId(
                ProgramStatus.DRAFT, 1L))
                .thenReturn(List.of(new Program()));

        List<Program> result =
                programService.getDraftProgramsByHost();

        assertEquals(1, result.size());
    }

    @Test
    void getDraftProgramsByHost_noAuth_shouldThrowUserNotFound() {

        SecurityContextHolder.clearContext();

        assertThrows(
                UserNotFoundException.class,
                () -> programService.getDraftProgramsByHost()
        );
    }


    @Test
    void changeProgramStatusToActive_programNotFound() {

        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L))
                .thenAnswer(invocation -> Optional.of(user));

        when(programRepository.findById(any()))
                .thenAnswer(invocation -> Optional.empty());

        UUID programId = UUID.randomUUID();

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> programService.changeProgramStatusToActive(programId)
        );

        assertEquals("Program not found", ex.getMessage());
    }




    @Test
    void changeProgramStatusToActive_invalidStatus_shouldThrowConflict() {

        mockAuthenticatedUser(1L);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setUser(user);
        program.setStatus(ProgramStatus.ACTIVE); // ❌ invalid (must be DRAFT)

        UUID programId = UUID.randomUUID(); // ✅ created outside lambda

        when(userRepository.findById(1L))
                .thenAnswer(invocation -> Optional.of(user));

        when(programRepository.findById(programId))
                .thenAnswer(invocation -> Optional.of(program));

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> programService.changeProgramStatusToActive(programId)
        );

        assertEquals(
                "Program status must be DRAFT to change to ACTIVE",
                ex.getMessage()
        );
    }







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
