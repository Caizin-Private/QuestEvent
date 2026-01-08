package com.questevent.service;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.dto.UserPrincipal;
import com.questevent.enums.Role;
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

    // --------------------------------------------------------
    // REGISTER
    // --------------------------------------------------------

    @Test
    void registerParticipantForProgram_success() {
        mockAuthenticatedUser(1L);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");

        ProgramRegistration saved = new ProgramRegistration();
        saved.setProgramRegistrationId(1L);
        saved.setProgram(program);
        saved.setUser(user);
        saved.setRegisteredAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(programRegistrationRepository.save(any()))
                .thenReturn(saved);

        ProgramRegistrationResponseDTO result =
                programRegistrationService.registerParticipantForProgram(request);

        assertNotNull(result);
        assertEquals(1L, result.getProgramRegistrationId());
        assertEquals(1L, result.getProgramId());
        assertEquals(1L, result.getUserId());

        verify(programWalletService)
                .createWallet(1L, 1L);
    }

    @Test
    void registerParticipantForProgram_userNotFound() {
        mockAuthenticatedUser(1L);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService
                        .registerParticipantForProgram(request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void registerParticipantForProgram_programNotFound() {
        mockAuthenticatedUser(1L);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(99L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(programRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService
                        .registerParticipantForProgram(request)
        );

        assertEquals("Program not found", ex.getMessage());
    }

    // --------------------------------------------------------
    // READ
    // --------------------------------------------------------

    @Test
    void getAllRegistrations_success() {
        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Program 1");

        User user = new User();
        user.setUserId(1L);
        user.setName("User 1");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(1L);
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
        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Program");

        User user = new User();
        user.setUserId(1L);
        user.setName("User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(1L);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findById(1L))
                .thenReturn(Optional.of(reg));

        ProgramRegistrationDTO dto =
                programRegistrationService.getRegistrationById(1L);

        assertEquals(1L, dto.getProgramRegistrationId());
    }

    @Test
    void getRegistrationById_notFound() {
        when(programRegistrationRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.getRegistrationById(99L)
        );

        assertEquals("Registration not found with id: 99", ex.getMessage());
    }

    // --------------------------------------------------------
    // DELETE
    // --------------------------------------------------------

    @Test
    void deleteRegistration_success() {
        when(programRegistrationRepository.existsById(1L))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                programRegistrationService.deleteRegistration(1L));

        verify(programRegistrationRepository)
                .deleteById(1L);
    }

    @Test
    void deleteRegistration_notFound() {
        when(programRegistrationRepository.existsById(99L))
                .thenReturn(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.deleteRegistration(99L)
        );

        assertEquals("Registration not found with id: 99", ex.getMessage());
    }

    // --------------------------------------------------------
    // COUNT
    // --------------------------------------------------------

    @Test
    void getParticipantCountForProgram_success() {
        when(programRegistrationRepository
                .countByProgramProgramId(1L))
                .thenReturn(5L);

        long count =
                programRegistrationService
                        .getParticipantCountForProgram(1L);

        assertEquals(5L, count);
    }
}
