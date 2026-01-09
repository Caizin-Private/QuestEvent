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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    /* ===================== mocks ===================== */

    @Mock private UserRepository userRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ProgramRegistrationRepository programRegistrationRepository;
    @Mock private ActivityRegistrationRepository activityRegistrationRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ProgramWalletRepository programWalletRepository;
    @Mock private JudgeRepository judgeRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private RbacService rbacService;

    /* ===================== fixtures ===================== */

    private User owner;
    private User user;
    private User judgeUser;

    private Program program;
    private Activity activity;

    @BeforeEach
    void setup() {
        owner = createUser(1L, Role.OWNER);
        user = createUser(2L, Role.USER);
        judgeUser = createUser(3L, Role.JUDGE);

        program = new Program();
        program.setProgramId(10L);
        program.setUser(owner);

        Judge judge = new Judge();
        judge.setUser(judgeUser);
        program.setJudge(judge);

        activity = new Activity();
        activity.setActivityId(20L);
        activity.setProgram(program);
    }

    /* ===================== helpers ===================== */

    private User createUser(Long id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return u;
    }

    private void authenticate(User u) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal())
                .thenReturn(new UserPrincipal(u.getUserId(), "test@test.com", u.getRole()));
        when(userRepository.findById(u.getUserId()))
                .thenReturn(Optional.of(u));
    }

    /* ===================================================
       AUTHENTICATION BRANCHES (BIGGEST COVERAGE GAIN)
       =================================================== */

    @Test
    void unauthenticated_user_denied_everywhere() {
        when(authentication.isAuthenticated()).thenReturn(false);

        assertFalse(rbacService.canViewProgram(authentication));
        assertFalse(rbacService.canAccessUserProfile(authentication, 1L));
        assertFalse(rbacService.canManageProgram(authentication, 1L));
        assertFalse(rbacService.canJudgeAccessProgram(authentication, 1L));
        assertFalse(rbacService.canRegisterForProgram(authentication, 1L, 1L));
        assertFalse(rbacService.canRegisterForActivity(authentication, 1L, 1L));
        assertFalse(rbacService.canSubmitActivity(authentication, 1L, 1L));
    }

    @Test
    void jwtAuthentication_branch_covered() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "jwt@test.com")
                .build();

        JwtAuthenticationToken jwtAuth =
                new JwtAuthenticationToken(jwt, List.of(() -> "ROLE_USER"));

        when(userRepository.findByEmail("jwt@test.com"))
                .thenReturn(Optional.of(user));

        assertTrue(rbacService.canViewProgram(jwtAuth));
    }


    @Test
    void oauth2User_email_branch_covered() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email"))
                .thenReturn("oauth@test.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(userRepository.findByEmail("oauth@test.com"))
                .thenReturn(Optional.of(user));

        assertTrue(rbacService.canViewProgram(authentication));
    }

    @Test
    void oauth2User_preferredUsername_fallback_covered() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("email")).thenReturn(null);
        when(oauthUser.getAttribute("preferred_username"))
                .thenReturn("fallback@test.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(userRepository.findByEmail("fallback@test.com"))
                .thenReturn(Optional.of(user));

        assertTrue(rbacService.canViewProgram(authentication));
    }

    /* ===================================================
       PLATFORM OWNER SHORT-CIRCUITS
       =================================================== */

    @Test
    void owner_short_circuit_paths() {
        authenticate(owner);

        assertTrue(rbacService.isPlatformOwner(authentication));
        assertTrue(rbacService.canViewProgram(authentication));
        assertTrue(rbacService.canJudgeAccessProgram(authentication, 1L));
        assertTrue(rbacService.canAccessUserWallet(authentication, 99L));
        assertTrue(rbacService.canAccessProgramWallet(authentication, UUID.randomUUID()));
    }

    /* ===================================================
       USER PROFILE
       =================================================== */

    @Test
    void user_profile_paths() {
        authenticate(user);

        assertTrue(rbacService.canAccessUserProfile(authentication, 2L));
        assertFalse(rbacService.canAccessUserProfile(authentication, 99L));
    }

    /* ===================================================
       PROGRAM MANAGEMENT
       =================================================== */

    @Test
    void manage_program_paths() {
        authenticate(user);

        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));
        assertFalse(rbacService.canManageProgram(authentication, 10L));

        program.setUser(user);
        assertTrue(rbacService.canManageProgram(authentication, 10L));

        when(programRepository.findById(99L))
                .thenReturn(Optional.empty());
        assertFalse(rbacService.canManageProgram(authentication, 99L));
    }

    /* ===================================================
       JUDGE ACCESS
       =================================================== */

    @Test
    void judge_paths() {
        authenticate(judgeUser);

        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));
        assertTrue(rbacService.isJudgeForProgram(authentication, 10L));

        when(activityRepository.findById(20L))
                .thenReturn(Optional.of(activity));
        assertTrue(rbacService.isJudgeForActivity(authentication, 20L));

        when(judgeRepository.findByUserUserId(3L))
                .thenReturn(Optional.of(new Judge()));
        assertTrue(rbacService.canAccessJudgeSubmissions(authentication));

        when(judgeRepository.findByUserUserId(3L))
                .thenReturn(Optional.empty());
        assertFalse(rbacService.canAccessJudgeSubmissions(authentication));
    }

    @Test
    void programWithoutJudge_judgeAccessDenied() {
        authenticate(judgeUser);

        program.setJudge(null);
        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        assertFalse(rbacService.canJudgeAccessProgram(authentication, 10L));
    }

    /* ===================================================
       PROGRAM REGISTRATION
       =================================================== */

    @Test
    void program_registration_paths() {
        authenticate(user);

        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(10L, 2L))
                .thenReturn(false);
        assertTrue(rbacService.canRegisterForProgram(authentication, 10L, 2L));

        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(10L, 2L))
                .thenReturn(true);
        assertFalse(rbacService.canRegisterForProgram(authentication, 10L, 2L));

        assertFalse(rbacService.canRegisterForProgram(authentication, 10L, 99L));
    }

    /* ===================================================
       ACTIVITY REGISTRATION
       =================================================== */

    @Test
    void activity_registration_paths() {
        authenticate(user);

        when(activityRepository.findById(20L))
                .thenReturn(Optional.of(activity));
        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(10L, 2L))
                .thenReturn(true);
        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(20L, 2L))
                .thenReturn(false);

        assertTrue(rbacService.canRegisterForActivity(authentication, 20L, 2L));

        when(activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(20L, 2L))
                .thenReturn(true);
        assertFalse(rbacService.canRegisterForActivity(authentication, 20L, 2L));

        when(programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(10L, 2L))
                .thenReturn(false);
        assertFalse(rbacService.canRegisterForActivity(authentication, 20L, 2L));
    }

    @Test
    void activity_without_program_denied() {
        authenticate(user);

        Activity a = new Activity();
        a.setActivityId(99L);
        a.setProgram(null);

        when(activityRepository.findById(99L))
                .thenReturn(Optional.of(a));

        assertFalse(rbacService.canRegisterForActivity(authentication, 99L, 2L));
    }

    /* ===================================================
       SUBMISSIONS
       =================================================== */

    @Test
    void submission_paths() {
        authenticate(user);

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(30L);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(20L, 2L))
                .thenReturn(Optional.of(reg));
        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(30L))
                .thenReturn(false);

        assertTrue(rbacService.canSubmitActivity(authentication, 20L, 2L));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(30L))
                .thenReturn(true);
        assertFalse(rbacService.canSubmitActivity(authentication, 20L, 2L));
    }

    /* ===================================================
       WALLETS
       =================================================== */

    @Test
    void wallet_paths() {
        authenticate(user);

        assertTrue(rbacService.canAccessUserWallet(authentication, 2L));
        assertFalse(rbacService.canAccessUserWallet(authentication, 99L));

        program.setUser(user);
        when(programRepository.findById(10L))
                .thenReturn(Optional.of(program));

        assertTrue(rbacService.canAccessMyProgramWallet(authentication, 10L));
        assertTrue(rbacService.canAccessProgramWalletsByProgram(authentication, 10L));
    }

    @Test
    void programWallet_notFound_denied() {
        authenticate(user);

        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canAccessProgramWallet(
                        authentication,
                        UUID.randomUUID()
                )
        );
    }

    /* ===================================================
       UTIL
       =================================================== */

    @Test
    void getProgramIdByActivityId_paths() {
        when(activityRepository.findById(20L))
                .thenReturn(Optional.of(activity));
        assertEquals(10L, rbacService.getProgramIdByActivityId(20L));

        when(activityRepository.findById(99L))
                .thenReturn(Optional.empty());
        assertNull(rbacService.getProgramIdByActivityId(99L));
    }
}
