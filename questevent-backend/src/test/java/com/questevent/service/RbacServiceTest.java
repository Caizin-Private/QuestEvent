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

    @Mock private UserRepository userRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ActivityRegistrationRepository activityRegistrationRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ProgramWalletRepository programWalletRepository;
    @Mock private JudgeRepository judgeRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private RbacService rbacService;

    private User owner;
    private User user;
    private User judgeUser;

    private Program program;
    private Activity activity;

    private UUID programId;
    private UUID activityId;

    @BeforeEach
    void setup() {

        programId = UUID.randomUUID();
        activityId = UUID.randomUUID();

        owner = createUser(1L, Role.OWNER);
        user = createUser(2L, Role.USER);
        judgeUser = createUser(3L, Role.JUDGE);

        program = new Program();
        program.setProgramId(programId);
        program.setUser(owner);

        Judge judge = new Judge();
        judge.setUser(judgeUser);
        program.setJudge(judge);

        activity = new Activity();
        activity.setActivityId(activityId);
        activity.setProgram(program);
    }

    private User createUser(Long id, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role);
        return u;
    }

    private void authenticate(User u) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal())
                .thenReturn(new UserPrincipal(
                        u.getUserId(),
                        "test@test.com",
                        u.getRole()
                ));
        when(userRepository.findById(u.getUserId()))
                .thenReturn(Optional.of(u));
    }

    @Test
    void unauthenticated_user_denied() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertFalse(rbacService.canViewProgram(authentication));
    }

    @Test
    void jwt_authentication_branch_covered() {

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
    void oauth2_email_branch_covered() {

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
    void oauth2_fallback_username_branch_covered() {

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

    @Test
    void owner_short_circuit_paths() {

        authenticate(owner);

        assertTrue(rbacService.isPlatformOwner(authentication));
        assertTrue(rbacService.canViewProgram(authentication));
        assertTrue(rbacService.canManageProgram(authentication, programId));
        assertTrue(rbacService.canJudgeAccessProgram(authentication, programId));
    }

    @Test
    void program_management_paths() {

        authenticate(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertFalse(rbacService.canManageProgram(authentication, programId));

        program.setUser(user);
        assertTrue(rbacService.canManageProgram(authentication, programId));

        when(programRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canManageProgram(authentication, UUID.randomUUID())
        );
    }

    @Test
    void verify_submission_paths() {

        authenticate(judgeUser);

        ActivityRegistration reg = new ActivityRegistration();
        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(reg);

        Activity act = new Activity();
        act.setProgram(program);
        reg.setActivity(act);

        when(submissionRepository.findById(any()))
                .thenReturn(Optional.of(submission));

        assertTrue(
                rbacService.canVerifySubmission(authentication, UUID.randomUUID())
        );
    }

    @Test
    void getProgramIdByActivityId_paths() {

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        assertEquals(
                programId,
                rbacService.getProgramIdByActivityId(activityId)
        );

        when(activityRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertNull(
                rbacService.getProgramIdByActivityId(UUID.randomUUID())
        );
    }
}
