package com.questevent.service;

import com.questevent.dto.ProgramRequestDTO;
import com.questevent.entity.Judge;
import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import com.questevent.exception.ProgramNotFoundException;
import com.questevent.exception.ResourceConflictException;
import com.questevent.exception.UserNotFoundException;
import com.questevent.repository.JudgeRepository;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.UserRepository;
import com.questevent.utils.SecurityUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JudgeRepository judgeRepository;

    @Mock
    private ProgramRegistrationRepository programRegistrationRepository;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @InjectMocks
    private ProgramService service;

    private User host;
    private User judgeUser;
    private Program program;
    private UUID programId;

    @BeforeEach
    void setUp() {
        programId = UUID.randomUUID();

        host = new User();
        host.setUserId(1L);

        judgeUser = new User();
        judgeUser.setUserId(2L);

        program = new Program();
        program.setProgramId(programId);
        program.setUser(host);
        program.setStatus(ProgramStatus.DRAFT);
    }

    @Test
    void createProgram_success() {
        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(judgeUser.getUserId());

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(userRepository.findById(judgeUser.getUserId()))
                .thenReturn(Optional.of(judgeUser));
        when(programRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Program result = service.createProgram(dto);

        assertThat(result.getUser()).isEqualTo(host);
        assertThat(result.getJudge().getUser()).isEqualTo(judgeUser);
    }

    @Test
    void createProgram_fails_whenHostIsJudge() {
        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(host.getUserId());

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(userRepository.findById(host.getUserId()))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() -> service.createProgram(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createProgram_fails_whenJudgeNotFound() {
        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setJudgeUserId(99L);

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProgram(dto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProgram_success() {
        ProgramRequestDTO dto = new ProgramRequestDTO();
        dto.setProgramTitle("Updated");

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.save(program))
                .thenReturn(program);

        Program updated = service.updateProgram(programId, dto);

        assertThat(updated.getProgramTitle()).isEqualTo("Updated");
    }

    @Test
    void updateProgram_fails_whenNotOwner() {
        User other = new User();
        other.setUserId(99L);

        when(securityUserResolver.getCurrentUser()).thenReturn(other);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertThatThrownBy(() ->
                service.updateProgram(programId, new ProgramRequestDTO()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteProgram_success() {
        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        service.deleteProgram(programId);

        verify(programRepository).delete(program);
    }

    @Test
    void deleteProgram_fails_whenNotOwner() {
        User other = new User();
        other.setUserId(99L);

        when(securityUserResolver.getCurrentUser()).thenReturn(other);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.deleteProgram(programId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void changeProgramStatusToActive_success() {
        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.save(program))
                .thenReturn(program);

        Program updated = service.changeProgramStatusToActive(programId);

        assertThat(updated.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
    }

    @Test
    void changeProgramStatusToActive_fails_whenNotDraft() {
        program.setStatus(ProgramStatus.ACTIVE);

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertThatThrownBy(() ->
                service.changeProgramStatusToActive(programId))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void getCompletedProgramsForUser() {
        Program completed = new Program();
        completed.setStatus(ProgramStatus.COMPLETED);

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgram(completed);

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRegistrationRepository.findByUserUserId(host.getUserId()))
                .thenReturn(List.of(reg));

        List<Program> result = service.getCompletedProgramsForUser();

        assertThat(result).containsExactly(completed);
    }

    @Test
    void getProgramsWhereUserIsJudge() {
        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findByJudgeUserId(host.getUserId()))
                .thenReturn(List.of(program));

        assertThat(service.getProgramsWhereUserIsJudge()).containsExactly(program);
    }

    @Test
    void getDraftProgramsByHost() {
        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(programRepository.findByStatusAndUser_UserId(
                ProgramStatus.DRAFT, host.getUserId()))
                .thenReturn(List.of(program));

        assertThat(service.getDraftProgramsByHost()).containsExactly(program);
    }

    @Test
    void getActiveProgramsByUserDepartment() {
        host.setDepartment(Department.IT);

        when(securityUserResolver.getCurrentUser()).thenReturn(host);
        when(userRepository.findById(host.getUserId()))
                .thenReturn(Optional.of(host));
        when(programRepository.findByStatusAndDepartment(
                ProgramStatus.ACTIVE, Department.IT))
                .thenReturn(List.of(program));

        assertThat(service.getActiveProgramsByUserDepartment())
                .containsExactly(program);
    }

    @Test
    void getProgramById_fails_whenNotFound() {
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getProgramById(programId))
                .isInstanceOf(ProgramNotFoundException.class);
    }
}
