package com.questevent.rbac;

import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    private User owner;
    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@company.com");
        owner.setRole(Role.OWNER);

        user = new User();
        user.setUserId(2L);
        user.setEmail("user@company.com");
        user.setRole(Role.USER);
    }

    private Authentication auth(String email) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("email", email)
        );

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(jwt, null);

        authentication.setAuthenticated(true);

        return authentication;
    }

    @Test
    void isPlatformOwner_true_forOwner() {
        when(userRepository.findByEmail("owner@company.com"))
                .thenReturn(Optional.of(owner));

        assertThat(rbacService.isPlatformOwner(auth("owner@company.com")))
                .isTrue();
    }

    @Test
    void isPlatformOwner_false_forUser() {
        when(userRepository.findByEmail("user@company.com"))
                .thenReturn(Optional.of(user));

        assertThat(rbacService.isPlatformOwner(auth("user@company.com")))
                .isFalse();
    }

    @Test
    void canAccessUserProfile_owner_canAccessAnyUser() {
        when(userRepository.findByEmail("owner@company.com"))
                .thenReturn(Optional.of(owner));

        assertThat(
                rbacService.canAccessUserProfile(auth("owner@company.com"), 999L)
        ).isTrue();
    }

    @Test
    void canAccessUserProfile_user_canAccessSelf() {
        when(userRepository.findByEmail("user@company.com"))
                .thenReturn(Optional.of(user));

        assertThat(
                rbacService.canAccessUserProfile(auth("user@company.com"), 2L)
        ).isTrue();
    }

    @Test
    void canAccessUserProfile_user_cannotAccessOthers() {
        when(userRepository.findByEmail("user@company.com"))
                .thenReturn(Optional.of(user));

        assertThat(
                rbacService.canAccessUserProfile(auth("user@company.com"), 1L)
        ).isFalse();
    }

    @Test
    void canAccessUserWallet_delegatesToProfileLogic() {
        when(userRepository.findByEmail("user@company.com"))
                .thenReturn(Optional.of(user));

        assertThat(
                rbacService.canAccessUserWallet(auth("user@company.com"), 2L)
        ).isTrue();
    }

    @Test
    void returnsFalse_whenAuthenticationIsNull() {
        assertThat(rbacService.isPlatformOwner(null)).isFalse();
    }

    @Test
    void returnsFalse_whenPrincipalIsNotJwt() {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("user", null);
        auth.setAuthenticated(true);

        assertThat(rbacService.isPlatformOwner(auth)).isFalse();
    }
}
