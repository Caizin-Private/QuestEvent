package com.questevent.service;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    ProgramRegistrationServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    private void authenticate(Long userId) {
        UserPrincipal principal =
                new UserPrincipal(userId, "test@questevent.com", Role.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @Test
    void registerParticipantForProgram_success() {
        authenticate(1L);

        Program program = new Program();
        program.setProgramId(10L);
        program.setProgramTitle("Program");

        User user = new User();
        user.setUserId(1L);
        user.setEmail("test@questevent.com");

        ProgramRegistration saved = new ProgramRegistration();
        saved.setProgramRegistrationId(100L);
        saved.setProgram(program);
        saved.setUser(user);
        saved.setRegisteredAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(10L, 1L))
                .thenReturn(false);
        when(programRegistrationRepository.save(any()))
                .thenReturn(saved);

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO();
        request.setProgramId(10L);

        ProgramRegistrationResponseDTO result =
                programRegistrationService.registerParticipantForProgram(request);

        assertEquals(100L, result.getProgramRegistrationId());
        assertEquals(10L, result.getProgramId());
        assertEquals(1L, result.getUserId());

        verify(programWalletService).createWallet(1L, 10L);
    }

    @Test
    void getAllRegistrations_success() {
        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Program");

        User user = new User();
        user.setUserId(2L);
        user.setName("User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(5L);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findAll())
                .thenReturn(List.of(reg));

        List<ProgramRegistrationDTO> result =
                programRegistrationService.getAllRegistrations();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProgramId());
        assertEquals(2L, result.get(0).getUserId());
    }

    @Test
    void getRegistrationById_success() {
        Program program = new Program();
        program.setProgramId(3L);
        program.setProgramTitle("My Program");

        User user = new User();
        user.setUserId(4L);
        user.setName("User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(9L);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findById(9L))
                .thenReturn(Optional.of(reg));

        ProgramRegistrationDTO dto =
                programRegistrationService.getRegistrationById(9L);

        assertEquals(9L, dto.getProgramRegistrationId());
        assertEquals(3L, dto.getProgramId());
        assertEquals(4L, dto.getUserId());
    }

    @Test
    void getRegistrationById_notFound() {
        when(programRegistrationRepository.findById(99L))
                .thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> programRegistrationService.getRegistrationById(99L)
        );

        assertTrue(ex.getMessage().contains("Registration not found"));
    }

    @Test
    void deleteRegistration_success() {
        when(programRegistrationRepository.existsById(1L))
                .thenReturn(true);

        programRegistrationService.deleteRegistration(1L);

        verify(programRegistrationRepository).deleteById(1L);
    }

    @Test
    void deleteRegistration_notFound() {
        when(programRegistrationRepository.existsById(99L))
                .thenReturn(false);

        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> programRegistrationService.deleteRegistration(99L)
        );

        assertTrue(ex.getMessage().contains("Registration not found"));
    }

    @Test
    void getParticipantCountForProgram_success() {
        when(programRegistrationRepository.countByProgramProgramId(10L))
                .thenReturn(7L);

        long count =
                programRegistrationService.getParticipantCountForProgram(10L);

        assertEquals(7L, count);
    }
}
