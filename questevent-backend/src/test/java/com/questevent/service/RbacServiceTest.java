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
    @Mock private ProgramRegistrationRepository programRegistrationRepository;
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

    private Long ownerId;
    private Long userId;
    private Long judgeUserId;
    private UUID programId;
    private UUID activityId;

    @BeforeEach
    void setup() {

        ownerId = 1L;
        userId = 2L;
        judgeUserId = 3L;

        programId = UUID.randomUUID();
        activityId = UUID.randomUUID();

        owner = createUser(ownerId, Role.OWNER);
        user = createUser(userId, Role.USER);
        judgeUser = createUser(judgeUserId, Role.JUDGE);

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
    void unauthenticated_user_denied_everywhere() {

        when(authentication.isAuthenticated()).thenReturn(false);

        assertFalse(rbacService.canViewProgram(authentication));
        assertFalse(rbacService.canAccessUserProfile(authentication, userId));
        assertFalse(rbacService.canManageProgram(authentication, programId));
        assertFalse(rbacService.canJudgeAccessProgram(authentication, programId));
        assertFalse(rbacService.canRegisterForProgram(authentication, programId, userId));
        assertFalse(rbacService.canRegisterForActivity(authentication, activityId, userId));
        assertFalse(rbacService.canSubmitActivity(authentication, activityId, userId));
    }
    @Test
    void unknownPrincipal_returnsFalseEverywhere() {

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("some-string");

        assertFalse(rbacService.canViewProgram(authentication));
        assertFalse(rbacService.canAccessUserProfile(authentication, 1L));
    }

    @Test
    void jwt_preferredUsername_fallback_used() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "pref@test.com")
                .build();

        JwtAuthenticationToken jwtAuth =
                new JwtAuthenticationToken(jwt, List.of());

        when(userRepository.findByEmail("pref@test.com"))
                .thenReturn(Optional.of(user));

        assertTrue(rbacService.canViewProgram(jwtAuth));
    }

    @Test
    void jwt_upn_fallback_used() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("upn", "upn@test.com")
                .build();

        JwtAuthenticationToken jwtAuth =
                new JwtAuthenticationToken(jwt, List.of());

        when(userRepository.findByEmail("upn@test.com"))
                .thenReturn(Optional.of(user));

        assertTrue(rbacService.canViewProgram(jwtAuth));
    }

    @Test
    void canAccessActivityRegistration_activityNull_returnsFalse() {

        authenticate(user);

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivity(null);

        when(activityRegistrationRepository.findById(any()))
                .thenReturn(Optional.of(reg));

        assertFalse(
                rbacService.canAccessActivityRegistration(authentication, UUID.randomUUID())
        );
    }

    @Test
    void canAccessProgramRegistration_programNull_returnsFalse() {

        authenticate(user);

        ProgramRegistration reg = new ProgramRegistration();
        reg.setProgram(null);

        when(programRegistrationRepository.findById(any()))
                .thenReturn(Optional.of(reg));

        assertFalse(
                rbacService.canAccessProgramRegistration(authentication, UUID.randomUUID())
        );
    }

    @Test
    void canVerifySubmission_activityNull_returnsFalse() {

        authenticate(user);

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivity(null);

        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityRegistration(reg);

        when(submissionRepository.findById(any()))
                .thenReturn(Optional.of(submission));

        assertFalse(
                rbacService.canVerifySubmission(authentication, UUID.randomUUID())
        );
    }
    @Test
    void canAccessProgramWallet_walletUserMismatch_returnsFalse() {

        authenticate(user);

        ProgramWallet wallet = new ProgramWallet();
        wallet.setUser(owner); // different user

        when(programWalletRepository.findById(any()))
                .thenReturn(Optional.of(wallet));

        assertFalse(
                rbacService.canAccessProgramWallet(authentication, UUID.randomUUID())
        );
    }

    @Test
    void canAccessMyProgramWallet_walletExists_returnsTrue() {

        authenticate(user);

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(userId, programId))
                .thenReturn(Optional.of(new ProgramWallet()));

        assertTrue(
                rbacService.canAccessMyProgramWallet(authentication, programId)
        );
    }

    @Test
    void canRegisterForProgram_ownerAlreadyRegistered_returnsFalse() {

        authenticate(user);

        program.setUser(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        when(programRegistrationRepository
                .existsByProgramProgramIdAndUserUserId(programId, userId))
                .thenReturn(true);

        assertFalse(
                rbacService.canRegisterForProgram(authentication, programId, userId)
        );
    }

    @Test
    void canRegisterForActivity_duplicateActivityRegistration_returnsFalse() {

        authenticate(user);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        when(programRegistrationRepository
                .existsByProgramProgramIdAndUserUserId(programId, userId))
                .thenReturn(true);

        when(activityRegistrationRepository
                .existsByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(true);

        assertFalse(
                rbacService.canRegisterForActivity(authentication, activityId, userId)
        );
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
    void jwtAuthentication_userNotFound_returnsFalse() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "missing@test.com")
                .build();

        JwtAuthenticationToken jwtAuth =
                new JwtAuthenticationToken(jwt, List.of());

        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertFalse(rbacService.canViewProgram(jwtAuth));
    }

    @Test
    void oauth2User_allAttributesNull_returnsFalse() {

        OAuth2User oauthUser = mock(OAuth2User.class);

        when(oauthUser.getAttribute(any()))
                .thenReturn(null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);

        assertFalse(rbacService.canViewProgram(authentication));
    }

    @Test
    void canAccessProgramWalletsByProgram_judgeAllowed() {

        authenticate(judgeUser);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertTrue(
                rbacService.canAccessProgramWalletsByProgram(authentication, programId)
        );
    }

    @Test
    void canRegisterForProgram_userIdMismatch_returnsFalse() {

        authenticate(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertFalse(
                rbacService.canRegisterForProgram(authentication, programId, 999L)
        );
    }

    @Test
    void canRegisterForActivity_programIdNull_returnsFalse() {

        authenticate(user);

        Activity activityWithoutProgram = new Activity();
        activityWithoutProgram.setActivityId(activityId);
        activityWithoutProgram.setProgram(null);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activityWithoutProgram));

        assertFalse(
                rbacService.canRegisterForActivity(authentication, activityId, userId)
        );
    }

    @Test
    void canSubmitActivity_roleNotUser_returnsFalse() {

        authenticate(owner);

        assertFalse(
                rbacService.canSubmitActivity(authentication, activityId, ownerId)
        );
    }

    @Test
    void canSubmitActivity_submissionAlreadyExists_returnsFalse() {

        authenticate(user);

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(UUID.randomUUID());

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.of(reg));

        when(submissionRepository
                .existsByActivityRegistrationActivityRegistrationId(
                        reg.getActivityRegistrationId()))
                .thenReturn(true);

        assertFalse(
                rbacService.canSubmitActivity(authentication, activityId, userId)
        );
    }

    @Test
    void isJudgeForActivity_programNull_returnsFalse() {

        authenticate(judgeUser);

        Activity activityWithoutProgram = new Activity();
        activityWithoutProgram.setActivityId(activityId);
        activityWithoutProgram.setProgram(null);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activityWithoutProgram));

        assertFalse(
                rbacService.isJudgeForActivity(authentication, activityId)
        );
    }


    @Test
    void canAccessMyProgramWallet_programUserMismatch_returnsFalse() {

        authenticate(user);

        Program otherProgram = new Program();
        otherProgram.setProgramId(programId);
        otherProgram.setUser(owner); // different user

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(otherProgram));

        when(programWalletRepository
                .findByUserUserIdAndProgramProgramId(userId, programId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canAccessMyProgramWallet(authentication, programId)
        );
    }


    @Test
    void canAccessProgramWalletsByProgram_programNotFound_returnsFalse() {

        authenticate(user);

        UUID pid = UUID.randomUUID();

        when(programRepository.findById(pid))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canAccessProgramWalletsByProgram(authentication, pid)
        );
    }

    @Test
    void canAccessProgramWalletsByProgram_programUserNull_returnsFalse() {

        authenticate(user);

        Program p = new Program();
        p.setProgramId(programId);
        p.setUser(null);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(p));

        assertFalse(
                rbacService.canAccessProgramWalletsByProgram(authentication, programId)
        );
    }

    @Test
    void canRegisterForProgram_programNotFound_returnsFalse() {

        authenticate(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canRegisterForProgram(authentication, programId, userId)
        );
    }


    @Test
    void canRegisterForActivity_programNotFound_returnsFalse() {

        authenticate(user);

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canRegisterForActivity(authentication, activityId, userId)
        );
    }


    @Test
    void canSubmitActivity_registrationNotFound_returnsFalse() {

        authenticate(user);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canSubmitActivity(authentication, activityId, userId)
        );
    }

    @Test
    void canVerifySubmission_submissionNotFound_returnsFalse() {

        authenticate(user);

        UUID submissionId = UUID.randomUUID();

        when(submissionRepository.findById(submissionId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canVerifySubmission(authentication, submissionId)
        );
    }

    @Test
    void canAccessProgramRegistration_notFound_returnsFalse() {

        authenticate(user);

        UUID regId = UUID.randomUUID();

        when(programRegistrationRepository.findById(regId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canAccessProgramRegistration(authentication, regId)
        );
    }

    @Test
    void canAccessActivityRegistration_notFound_returnsFalse() {

        authenticate(user);

        UUID regId = UUID.randomUUID();

        when(activityRegistrationRepository.findById(regId))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canAccessActivityRegistration(authentication, regId)
        );
    }

    @Test
    void canViewProgram_authenticationNull_returnsFalse() {

        assertFalse(
                rbacService.canViewProgram(null)
        );
    }

    @Test
    void canJudgeAccessProgram_judgeUserMismatch_returnsFalse() {

        authenticate(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program)); // judge is judgeUser

        assertFalse(
                rbacService.canJudgeAccessProgram(authentication, programId)
        );
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

    @Test
    void owner_short_circuit_paths() {

        authenticate(owner);

        assertTrue(rbacService.isPlatformOwner(authentication));
        assertTrue(rbacService.canViewProgram(authentication));
        assertTrue(rbacService.canJudgeAccessProgram(authentication, programId));
        assertTrue(rbacService.canAccessUserWallet(authentication, userId));
        assertTrue(rbacService.canAccessProgramWallet(authentication, UUID.randomUUID()));
    }

    @Test
    void user_profile_paths() {

        authenticate(user);

        assertTrue(rbacService.canAccessUserProfile(authentication, userId));
        assertFalse(rbacService.canAccessUserProfile(authentication, 999L));
    }

    @Test
    void manage_program_paths() {

        authenticate(user);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertFalse(rbacService.canManageProgram(authentication, programId));

        program.setUser(user);
        assertTrue(rbacService.canManageProgram(authentication, programId));

        when(programRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertFalse(
                rbacService.canManageProgram(authentication, UUID.randomUUID())
        );
    }

    @Test
    void judge_paths() {

        authenticate(judgeUser);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        assertTrue(rbacService.isJudgeForProgram(authentication, programId));

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));
        assertTrue(rbacService.isJudgeForActivity(authentication, activityId));

        when(judgeRepository.findByUserUserId(judgeUserId))
                .thenReturn(Optional.of(new Judge()));
        assertTrue(rbacService.canAccessJudgeSubmissions(authentication));

        when(judgeRepository.findByUserUserId(judgeUserId))
                .thenReturn(Optional.empty());
        assertFalse(rbacService.canAccessJudgeSubmissions(authentication));
    }

    @Test
    void submission_paths() {

        authenticate(user);

        UUID regId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(regId);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, userId))
                .thenReturn(Optional.of(reg));

        when(submissionRepository
                .existsByActivityRegistrationActivityRegistrationId(regId))
                .thenReturn(false);

        assertTrue(rbacService.canSubmitActivity(authentication, activityId, userId));
    }

    @Test
    void getProgramIdByActivityId_paths() {

        when(activityRepository.findById(activityId))
                .thenReturn(Optional.of(activity));

        assertEquals(
                programId,
                rbacService.getProgramIdByActivityId(activityId)
        );

        when(activityRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertNull(
                rbacService.getProgramIdByActivityId(UUID.randomUUID())
        );
    }
}
