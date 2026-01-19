package com.questevent.rbac;

import com.questevent.entity.*;
import com.questevent.enums.Role;
import com.questevent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProgramRepository programRepository;
    @Mock ActivityRepository activityRepository;
    @Mock ProgramRegistrationRepository programRegistrationRepository;
    @Mock ActivityRegistrationRepository activityRegistrationRepository;
    @Mock ActivitySubmissionRepository submissionRepository;
    @Mock ProgramWalletRepository programWalletRepository;

    @InjectMocks
    RbacService rbacService;

    private Authentication auth;
    private User normalUser;
    private User ownerUser;

    @BeforeEach
    void setup() {
        normalUser = new User();
        normalUser.setUserId(1L);
        normalUser.setEmail("user@test.com");
        normalUser.setRole(Role.USER);

        ownerUser = new User();
        ownerUser.setUserId(99L);
        ownerUser.setEmail("owner@test.com");
        ownerUser.setRole(Role.OWNER);
    }

    private Authentication jwtAuth(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(c -> c.put("preferred_username", email))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void isPlatformOwner_true_forOwner() {
        auth = jwtAuth("owner@test.com");
        when(userRepository.findByEmail("owner@test.com"))
                .thenReturn(Optional.of(ownerUser));

        assertThat(rbacService.isPlatformOwner(auth)).isTrue();
    }

    @Test
    void isPlatformOwner_false_forNormalUser() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        assertThat(rbacService.isPlatformOwner(auth)).isFalse();
    }

    @Test
    void canAccessUserProfile_selfAccess_allowed() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        assertThat(rbacService.canAccessUserProfile(auth, 1L)).isTrue();
    }

    @Test
    void canAccessUserProfile_otherUser_denied() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        assertThat(rbacService.canAccessUserProfile(auth, 2L)).isFalse();
    }

    @Test
    void canManageProgram_owner_allowed() {
        auth = jwtAuth("owner@test.com");
        when(userRepository.findByEmail("owner@test.com"))
                .thenReturn(Optional.of(ownerUser));

        assertThat(rbacService.canManageProgram(auth, UUID.randomUUID()))
                .isTrue();
    }

    @Test
    void canManageProgram_programOwner_allowed() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        Program program = new Program();
        program.setUser(normalUser);

        UUID programId = UUID.randomUUID();
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertThat(rbacService.canManageProgram(auth, programId))
                .isTrue();
    }

    @Test
    void canManageProgram_notOwner_denied() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        Program program = new Program();
        User other = new User();
        other.setUserId(2L);
        program.setUser(other);

        UUID programId = UUID.randomUUID();
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        assertThat(rbacService.canManageProgram(auth, programId))
                .isFalse();
    }

    @Test
    void canSubmitActivity_allowed_whenRegistered_andNotSubmitted() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        UUID activityId = UUID.randomUUID();
        UUID activityRegistrationId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(activityRegistrationId);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, 1L))
                .thenReturn(Optional.of(reg));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(activityRegistrationId))
                .thenReturn(false);

        assertThat(
                rbacService.canSubmitActivity(auth, activityId)
        ).isTrue();
    }


    @Test
    void canSubmitActivity_denied_ifAlreadySubmitted() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        UUID activityId = UUID.randomUUID();
        UUID activityRegistrationId = UUID.randomUUID();

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityRegistrationId(activityRegistrationId);

        when(activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(activityId, 1L))
                .thenReturn(Optional.of(reg));

        when(submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(activityRegistrationId))
                .thenReturn(true);

        assertThat(
                rbacService.canSubmitActivity(auth, activityId)
        ).isFalse();
    }

    @Test
    void canAccessProgramWallet_owner_allowed() {
        auth = jwtAuth("owner@test.com");
        when(userRepository.findByEmail("owner@test.com"))
                .thenReturn(Optional.of(ownerUser));

        assertThat(
                rbacService.canAccessProgramWallet(auth, UUID.randomUUID())
        ).isTrue();
    }

    @Test
    void canAccessProgramWallet_walletOwner_allowed() {
        auth = jwtAuth("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(normalUser));

        ProgramWallet wallet = new ProgramWallet();
        wallet.setUser(normalUser);

        UUID walletId = UUID.randomUUID();
        when(programWalletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        assertThat(
                rbacService.canAccessProgramWallet(auth, walletId)
        ).isTrue();
    }
}
