package com.questevent.service;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.*;
import com.questevent.enums.Role;
import com.questevent.rbac.RbacService;
import com.questevent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ProgramRegistrationRepository programRegistrationRepository;
    @Mock private ActivityRegistrationRepository activityRegistrationRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ProgramWalletRepository programWalletRepository;
    @Mock private JudgeRepository judgeRepository;

    @InjectMocks
    private RbacService rbacService;

    @Mock
    private Authentication authentication;

    private User owner;
    private User normalUser;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setUserId(1L);
        owner.setRole(Role.OWNER);

        normalUser = new User();
        normalUser.setUserId(2L);
        normalUser.setRole(Role.USER);
    }

    @Test
    void isPlatformOwner_true() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(1L, "o@test.com", Role.OWNER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertTrue(rbacService.isPlatformOwner(authentication));
    }

    @Test
    void canAccessUserProfile_owner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(1L, "o@test.com", Role.OWNER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertTrue(rbacService.canAccessUserProfile(authentication, 99L));
    }

    @Test
    void canAccessUserProfile_self() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(2L, "u@test.com", Role.USER));
        when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

        assertTrue(rbacService.canAccessUserProfile(authentication, 2L));
    }

    @Test
    void canManageProgram_owner() {
        Program program = new Program();
        program.setProgramId(10L);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(1L, "o@test.com", Role.OWNER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));

        assertTrue(rbacService.canManageProgram(authentication, 10L));
    }

    @Test
    void canViewProgram_authenticated() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(2L, "u@test.com", Role.USER));
        when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

        assertTrue(rbacService.canViewProgram(authentication, 5L));
    }

    @Test
    void canAccessUserWallet_owner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(1L, "o@test.com", Role.OWNER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertTrue(rbacService.canAccessUserWallet(authentication, 99L));
    }

    @Test
    void canAccessProgramWallet_owner() {
        UUID id = UUID.randomUUID();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(1L, "o@test.com", Role.OWNER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertTrue(rbacService.canAccessProgramWallet(authentication, id));
    }

    @Test
    void getProgramIdByActivityId_found() {
        Program program = new Program();
        program.setProgramId(5L);

        Activity activity = new Activity();
        activity.setActivityId(10L);
        activity.setProgram(program);

        when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

        assertEquals(5L, rbacService.getProgramIdByActivityId(10L));
    }

    @Test
    void getProgramIdByActivityId_notFound() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(rbacService.getProgramIdByActivityId(99L));
    }
}
