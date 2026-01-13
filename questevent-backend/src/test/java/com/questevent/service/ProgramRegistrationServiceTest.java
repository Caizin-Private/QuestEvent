package com.questevent.service;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgramRegistrationServiceTest {

    @Mock
    private ProgramRegistrationRepository programRegistrationRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramWalletService programWalletService;

    @InjectMocks
    private ProgramRegistrationService programRegistrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    // --------------------------------------------------------
    // Utility: mock authenticated user
    // --------------------------------------------------------
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
    void registerParticipantForProgram_success() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(programId);

        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");

        ProgramRegistration saved = new ProgramRegistration();
        saved.setProgramRegistrationId(registrationId);
        saved.setProgram(program);
        saved.setUser(user);
        saved.setRegisteredAt(Instant.now());

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(programId, userId))
                .thenReturn(false);
        when(programRegistrationRepository.save(any()))
                .thenReturn(saved);

        ProgramRegistrationResponseDTO result =
                programRegistrationService.registerParticipantForProgram(request);

        assertNotNull(result);
        assertEquals(registrationId, result.getProgramRegistrationId());
        assertEquals(programId, result.getProgramId());
        assertEquals(userId, result.getUserId());

        verify(programWalletService)
                .createWallet(userId, programId);
    }

    @Test
    void registerParticipantForProgram_userNotFound() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(programId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> programRegistrationService.registerParticipantForProgram(request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void registerParticipantForProgram_programNotFound() {

        Long userId = 1L;
        UUID programId = UUID.randomUUID();

        mockAuthenticatedUser(userId);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(programId);

        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ProgramNotFoundException ex = assertThrows(
                ProgramNotFoundException.class,
                () -> programRegistrationService.registerParticipantForProgram(request)
        );

        assertEquals("Program not found", ex.getMessage());
    }

    @Test
    void getAllRegistrations_success() {

        UUID programId = UUID.randomUUID();
        Long userId = 1L;
        UUID registrationId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Program 1");

        User user = new User();
        user.setUserId(userId);
        user.setName("User 1");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(registrationId);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findAll())
                .thenReturn(List.of(reg));

        List<ProgramRegistrationDTO> result =
                programRegistrationService.getAllRegistrations();

        assertEquals(1, result.size());
    }

    @Test
    void getRegistrationById_success() {

        UUID programId = UUID.randomUUID();
        Long userId = 1L;
        UUID registrationId = UUID.randomUUID();

        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Program");

        User user = new User();
        user.setUserId(userId);
        user.setName("User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(registrationId);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findById(registrationId))
                .thenReturn(Optional.of(reg));

        ProgramRegistrationDTO dto =
                programRegistrationService.getRegistrationById(registrationId);

        assertEquals(registrationId, dto.getProgramRegistrationId());
    }

    @Test
    void getRegistrationById_notFound() {

        UUID registrationId = UUID.randomUUID();

        when(programRegistrationRepository.findById(registrationId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> programRegistrationService.getRegistrationById(registrationId)
        );

        assertEquals(
                "Registration not found with id: " + registrationId,
                ex.getMessage()
        );
    }

    @Test
    void deleteRegistration_success() {

        UUID registrationId = UUID.randomUUID();

        when(programRegistrationRepository.existsById(registrationId))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                programRegistrationService.deleteRegistration(registrationId));

        verify(programRegistrationRepository)
                .deleteById(registrationId);
    }

    @Test
    void deleteRegistration_notFound() {

        UUID registrationId = UUID.randomUUID();

        when(programRegistrationRepository.existsById(registrationId))
                .thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> programRegistrationService.deleteRegistration(registrationId)
        );

        assertEquals(
                "Registration not found with id: " + registrationId,
                ex.getMessage()
        );
    }

    @Test
    void getParticipantCountForProgram_success() {

        UUID programId = UUID.randomUUID();

        when(programRegistrationRepository
                .countByProgramProgramId(programId))
                .thenReturn(5L);

        long count =
                programRegistrationService.getParticipantCountForProgram(programId);

        assertEquals(5L, count);
    }
}
