package com.questevent.service;

import com.questevent.dto.ProgramRegistrationDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    }

    @Test
    void registerParticipantForProgram_success() {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);
        request.setUserId(1L);

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");

        ProgramRegistration savedRegistration = new ProgramRegistration();
        savedRegistration.setProgramRegistrationId(1L);
        savedRegistration.setProgram(program);
        savedRegistration.setUser(user);
        savedRegistration.setRegisteredAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRegistrationRepository.existsByProgram_ProgramIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(programRegistrationRepository.save(any(ProgramRegistration.class)))
                .thenReturn(savedRegistration);

        ProgramWallet programWallet = new ProgramWallet();
        when(programWalletService.createWallet(1L, 1L)).thenReturn(programWallet);

        ProgramRegistrationResponseDTO result = programRegistrationService.registerParticipantForProgram(request);

        assertNotNull(result);
        assertEquals(1L, result.getProgramRegistrationId());
        assertEquals(1L, result.getProgramId());
        assertEquals(1L, result.getUserId());
        verify(programRegistrationRepository, times(1)).save(any(ProgramRegistration.class));
        verify(programWalletService, times(1)).createWallet(1L, 1L);
    }

    @Test
    void registerParticipantForProgram_userNotFound() {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);
        request.setUserId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.registerParticipantForProgram(request)
        );

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(programRegistrationRepository, never()).save(any());
        verify(programWalletService, never()).createWallet(any(), any());
    }

    @Test
    void registerParticipantForProgram_programNotFound() {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(999L);
        request.setUserId(1L);

        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.registerParticipantForProgram(request)
        );

        assertEquals("Program not found with id: 999", exception.getMessage());
        verify(programRegistrationRepository, never()).save(any());
        verify(programWalletService, never()).createWallet(any(), any());
    }

    @Test
    void registerParticipantForProgram_alreadyRegistered() {
        ProgramRegistrationRequestDTO request = new ProgramRegistrationRequestDTO();
        request.setProgramId(1L);
        request.setUserId(1L);

        User user = new User();
        user.setUserId(1L);

        Program program = new Program();
        program.setProgramId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(programRegistrationRepository.existsByProgram_ProgramIdAndUser_UserId(1L, 1L))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.registerParticipantForProgram(request)
        );

        assertEquals("User already registered for this program", exception.getMessage());
        verify(programRegistrationRepository, never()).save(any());
        verify(programWalletService, never()).createWallet(any(), any());
    }

    @Test
    void getAllRegistrations_success() {
        Program program1 = new Program();
        program1.setProgramId(1L);
        program1.setProgramTitle("Program 1");
        User user1 = new User();
        user1.setUserId(1L);
        user1.setName("User 1");

        Program program2 = new Program();
        program2.setProgramId(2L);
        program2.setProgramTitle("Program 2");
        User user2 = new User();
        user2.setUserId(2L);
        user2.setName("User 2");

        ProgramRegistration reg1 = new ProgramRegistration();
        reg1.setProgramRegistrationId(1L);
        reg1.setProgram(program1);
        reg1.setUser(user1);
        reg1.setRegisteredAt(LocalDateTime.now());

        ProgramRegistration reg2 = new ProgramRegistration();
        reg2.setProgramRegistrationId(2L);
        reg2.setProgram(program2);
        reg2.setUser(user2);
        reg2.setRegisteredAt(LocalDateTime.now());

        when(programRegistrationRepository.findAll()).thenReturn(List.of(reg1, reg2));

        List<ProgramRegistrationDTO> result = programRegistrationService.getAllRegistrations();

        assertEquals(2, result.size());
        verify(programRegistrationRepository, times(1)).findAll();
    }

    @Test
    void getRegistrationById_success() {
        Long id = 1L;
        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgramRegistrationId(id);

        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");

        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        registration.setProgram(program);
        registration.setUser(user);
        registration.setRegisteredAt(LocalDateTime.now());

        when(programRegistrationRepository.findById(id)).thenReturn(Optional.of(registration));

        ProgramRegistrationDTO result = programRegistrationService.getRegistrationById(id);

        assertNotNull(result);
        verify(programRegistrationRepository, times(1)).findById(id);
    }

    @Test
    void getRegistrationById_notFound() {
        Long id = 999L;

        when(programRegistrationRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.getRegistrationById(id)
        );

        assertEquals("Registration not found with id: 999", exception.getMessage());
    }

    @Test
    void getRegistrationsByProgramId_success() {
        Long programId = 1L;
        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");
        User user = new User();
        user.setUserId(1L);
        user.setName("Test User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(1L);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(LocalDateTime.now());

        when(programRegistrationRepository.findByProgramProgramId(programId))
                .thenReturn(List.of(reg));

        List<ProgramRegistrationDTO> result = programRegistrationService.getRegistrationsByProgramId(programId);

        assertEquals(1, result.size());
        verify(programRegistrationRepository, times(1)).findByProgramProgramId(programId);
    }

    @Test
    void getRegistrationsByUserId_success() {
        Long userId = 1L;
        Program program = new Program();
        program.setProgramId(1L);
        program.setProgramTitle("Test Program");
        User user = new User();
        user.setUserId(userId);
        user.setName("Test User");

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgramRegistrationId(1L);
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(LocalDateTime.now());

        when(programRegistrationRepository.findByUserUserId(userId))
                .thenReturn(List.of(reg));

        List<ProgramRegistrationDTO> result = programRegistrationService.getRegistrationsByUserId(userId);

        assertEquals(1, result.size());
        verify(programRegistrationRepository, times(1)).findByUserUserId(userId);
    }

    @Test
    void deleteRegistration_success() {
        Long id = 1L;

        when(programRegistrationRepository.existsById(id)).thenReturn(true);
        doNothing().when(programRegistrationRepository).deleteById(id);

        assertDoesNotThrow(() -> programRegistrationService.deleteRegistration(id));

        verify(programRegistrationRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteRegistration_notFound() {
        Long id = 999L;

        when(programRegistrationRepository.existsById(id)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> programRegistrationService.deleteRegistration(id)
        );

        assertEquals("Registration not found with id: 999", exception.getMessage());
        verify(programRegistrationRepository, never()).deleteById(any());
    }

    @Test
    void getParticipantCountForProgram_success() {
        Long programId = 1L;
        long count = 10L;

        when(programRegistrationRepository.countByProgramProgramId(programId)).thenReturn(count);

        long result = programRegistrationService.getParticipantCountForProgram(programId);

        assertEquals(count, result);
        verify(programRegistrationRepository, times(1)).countByProgramProgramId(programId);
    }
}
