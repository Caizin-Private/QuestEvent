package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.entity.Program;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramWalletService programWalletService;

    @InjectMocks
    private ProgramService programService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createProgram_success() {
        Long userId = 1L;
        User user = new User();
        user.setUserId(userId);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Test Program");
        dto.setProgramDescription("Test Description");
        dto.setDepartment(Department.IT);
        dto.setRegistrationFee(100);
        dto.setStatus(ProgramStatus.ACTIVE);

        Program savedProgram = new Program();
        savedProgram.setProgramId(1L);
        savedProgram.setProgramTitle("Test Program");
        savedProgram.setProgramDescription("Test Description");
        savedProgram.setDepartment(Department.IT);
        savedProgram.setRegistrationFee(100);
        savedProgram.setStatus(ProgramStatus.ACTIVE);
        savedProgram.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(programRepository.save(any(Program.class))).thenReturn(savedProgram);

        Program result = programService.createProgram(userId, dto);

        assertNotNull(result);
        assertEquals("Test Program", result.getProgramTitle());
        assertEquals(Department.IT, result.getDepartment());
        assertEquals(user, result.getUser());
        verify(programRepository, times(1)).save(any(Program.class));
    }

    @Test
    void createProgram_userNotFound() {
        Long userId = 999L;
        ProgramRequestDTO dto = new ProgramRequestDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.createProgram(userId, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(programRepository, never()).save(any());
    }

    @Test
    void updateProgram_success() {
        Long userId = 1L;
        Long programId = 1L;

        User user = new User();
        user.setUserId(userId);

        Program existingProgram = new Program();
        existingProgram.setProgramId(programId);
        existingProgram.setProgramTitle("Old Title");
        existingProgram.setUser(user);

        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("New Title");
        dto.setRegistrationFee(200);

        Program updatedProgram = new Program();
        updatedProgram.setProgramId(programId);
        updatedProgram.setProgramTitle("New Title");
        updatedProgram.setRegistrationFee(200);
        updatedProgram.setUser(user);

        when(programRepository.findById(programId)).thenReturn(Optional.of(existingProgram));
        when(programRepository.save(any(Program.class))).thenReturn(updatedProgram);

        Program result = programService.updateProgram(userId, programId, dto);

        assertNotNull(result);
        assertEquals("New Title", result.getProgramTitle());
        assertEquals(200, result.getRegistrationFee());
        verify(programRepository, times(1)).save(any(Program.class));
    }

    @Test
    void updateProgram_programNotFound() {
        Long userId = 1L;
        Long programId = 999L;
        ProgramRequestDTO dto = new ProgramRequestDTO();

        when(programRepository.findById(programId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(userId, programId, dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Program not found", exception.getReason());
        verify(programRepository, never()).save(any());
    }

    @Test
    void updateProgram_permissionDenied() {
        Long userId = 1L;
        Long programId = 1L;
        Long differentUserId = 2L;

        User differentUser = new User();
        differentUser.setUserId(differentUserId);

        Program existingProgram = new Program();
        existingProgram.setProgramId(programId);
        existingProgram.setUser(differentUser);

        ProgramRequestDTO dto = new ProgramRequestDTO();

        when(programRepository.findById(programId)).thenReturn(Optional.of(existingProgram));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.updateProgram(userId, programId, dto)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to update this program", exception.getReason());
        verify(programRepository, never()).save(any());
    }

    @Test
    void getProgramsByUserId_success() {
        Long userId = 1L;
        Program program1 = new Program();
        program1.setProgramId(1L);
        Program program2 = new Program();
        program2.setProgramId(2L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(programRepository.findByUser_UserId(userId))
                .thenReturn(List.of(program1, program2));

        List<Program> result = programService.getProgramsByUserId(userId);

        assertEquals(2, result.size());
        verify(programRepository, times(1)).findByUser_UserId(userId);
    }

    @Test
    void getProgramsByUserId_userNotFound() {
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.getProgramsByUserId(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(programRepository, never()).findByUser_UserId(any());
    }

    @Test
    void getProgramById_success() {
        Long programId = 1L;
        Program program = new Program();
        program.setProgramId(programId);
        program.setProgramTitle("Test Program");

        when(programRepository.findById(programId)).thenReturn(Optional.of(program));

        Program result = programService.getProgramById(programId);

        assertNotNull(result);
        assertEquals(programId, result.getProgramId());
        assertEquals("Test Program", result.getProgramTitle());
        verify(programRepository, times(1)).findById(programId);
    }

    @Test
    void getProgramById_notFound() {
        Long programId = 999L;

        when(programRepository.findById(programId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.getProgramById(programId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Program not found", exception.getReason());
    }

    @Test
    void getAllPrograms_success() {
        Program program1 = new Program();
        program1.setProgramId(1L);
        Program program2 = new Program();
        program2.setProgramId(2L);

        when(programRepository.findAll()).thenReturn(List.of(program1, program2));

        List<Program> result = programService.getAllPrograms();

        assertEquals(2, result.size());
        verify(programRepository, times(1)).findAll();
    }

    @Test
    void deleteProgram_success() {
        Long userId = 1L;
        Long programId = 1L;

        User user = new User();
        user.setUserId(userId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(user);

        when(programRepository.findById(programId)).thenReturn(Optional.of(program));
        doNothing().when(programRepository).delete(program);

        assertDoesNotThrow(() -> programService.deleteProgram(userId, programId));

        verify(programRepository, times(1)).delete(program);
    }

    @Test
    void deleteProgram_programNotFound() {
        Long userId = 1L;
        Long programId = 999L;

        when(programRepository.findById(programId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.deleteProgram(userId, programId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Program not found", exception.getReason());
        verify(programRepository, never()).delete(any());
    }

    @Test
    void deleteProgram_permissionDenied() {
        Long userId = 1L;
        Long programId = 1L;
        Long differentUserId = 2L;

        User differentUser = new User();
        differentUser.setUserId(differentUserId);

        Program program = new Program();
        program.setProgramId(programId);
        program.setUser(differentUser);

        when(programRepository.findById(programId)).thenReturn(Optional.of(program));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> programService.deleteProgram(userId, programId)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You do not have permission to delete this program", exception.getReason());
        verify(programRepository, never()).delete(any());
    }
}
