package com.questevent.service;

import com.questevent.dto.AddParticipantInProgramRequestDTO;
import com.questevent.dto.ProgramRegistrationRequestDTO;
import com.questevent.dto.ProgramRegistrationResponseDTO;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.ResourceNotFoundException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramRegistrationServiceTest {

    @Mock
    private ProgramRegistrationRepository programRegistrationRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramWalletService programWalletService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private ProgramRegistrationService service;

    private User user;
    private Program program;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setName("Maitreyee");
        user.setEmail("maitreyee@test.com");

        program = new Program();
        program.setProgramId(UUID.randomUUID());
        program.setProgramTitle("Quest Program");
    }

    /* =========================================================
       registerParticipantForProgram
       ========================================================= */

    @Test
    void registerParticipant_success() {

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO(program.getProgramId());

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(program.getProgramId(), user.getUserId()))
                .thenReturn(false);

        ProgramRegistration saved = new ProgramRegistration();
        saved.setProgram(program);
        saved.setUser(user);
        saved.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.save(any()))
                .thenReturn(saved);

        ProgramRegistrationResponseDTO response =
                service.registerParticipantForProgram(request);

        assertThat(response.getProgramId()).isEqualTo(program.getProgramId());
        assertThat(response.getUserId()).isEqualTo(user.getUserId());
        assertThat(response.getMessage())
                .isEqualTo("Successfully registered for program");

        verify(programWalletService)
                .createWallet(user.getUserId(), program.getProgramId());
    }

    @Test
    void registerParticipant_programNotFound() {

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO(UUID.randomUUID());

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(programRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.registerParticipantForProgram(request))
                .isInstanceOf(ProgramNotFoundException.class);
    }

    @Test
    void registerParticipant_duplicateRegistration() {

        ProgramRegistrationRequestDTO request =
                new ProgramRegistrationRequestDTO(program.getProgramId());

        when(securityUserResolver.getCurrentUser()).thenReturn(user);
        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(program.getProgramId(), user.getUserId()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.registerParticipantForProgram(request))
                .isInstanceOf(ResourceConflictException.class);
    }

    /* =========================================================
       addParticipantToProgram
       ========================================================= */

    @Test
    void addParticipant_success() {

        AddParticipantInProgramRequestDTO request =
                new AddParticipantInProgramRequestDTO();
        request.setUserId(2L);

        User participant = new User();
        participant.setUserId(2L);
        participant.setName("Participant");

        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(participant));
        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(program.getProgramId(), 2L))
                .thenReturn(false);

        ProgramRegistration saved = new ProgramRegistration();
        saved.setProgram(program);
        saved.setUser(participant);
        saved.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.save(any()))
                .thenReturn(saved);

        ProgramRegistrationResponseDTO response =
                service.addParticipantToProgram(program.getProgramId(), request);

        assertThat(response.getUserId()).isEqualTo(2L);
        verify(programWalletService)
                .createWallet(2L, program.getProgramId());
    }

    @Test
    void addParticipant_userNotFound() {

        AddParticipantInProgramRequestDTO request =
                new AddParticipantInProgramRequestDTO();
        request.setUserId(99L);

        when(programRepository.findById(program.getProgramId()))
                .thenReturn(Optional.of(program));
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.addParticipantToProgram(program.getProgramId(), request))
                .isInstanceOf(UserNotFoundException.class);
    }

    /* =========================================================
       get & delete
       ========================================================= */

    @Test
    void getAllRegistrations_success() {

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgram(program);
        reg.setUser(user);
        reg.setRegisteredAt(Instant.now());

        when(programRegistrationRepository.findAll())
                .thenReturn(List.of(reg));

        assertThat(service.getAllRegistrations()).hasSize(1);
    }

    @Test
    void getRegistrationById_notFound() {

        when(programRegistrationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getRegistrationById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRegistration_notFound() {

        when(programRegistrationRepository.existsById(any()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.deleteRegistration(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRegistration_success() {

        UUID id = UUID.randomUUID();
        when(programRegistrationRepository.existsById(id))
                .thenReturn(true);

        service.deleteRegistration(id);

        verify(programRegistrationRepository).deleteById(id);
    }

    /* =========================================================
       count
       ========================================================= */

    @Test
    void getParticipantCount_success() {

        when(programRegistrationRepository
                .countByProgramProgramId(program.getProgramId()))
                .thenReturn(5L);

        long count = service.getParticipantCountForProgram(program.getProgramId());

        assertThat(count).isEqualTo(5L);
    }
}
