package com.questevent.rbac;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.*;
import com.questevent.enums.Role;
import com.questevent.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("rbac")
public class RbacService {

    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final ActivityRepository activityRepository;
    private final ProgramRegistrationRepository programRegistrationRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ProgramWalletRepository programWalletRepository;
    private final JudgeRepository judgeRepository;

    public RbacService(
            UserRepository userRepository,
            ProgramRepository programRepository,
            ActivityRepository activityRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            ActivityRegistrationRepository activityRegistrationRepository,
            ActivitySubmissionRepository submissionRepository,
            ProgramWalletRepository programWalletRepository,
            JudgeRepository judgeRepository
    ) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.activityRepository = activityRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.activityRegistrationRepository = activityRegistrationRepository;
        this.submissionRepository = submissionRepository;
        this.programWalletRepository = programWalletRepository;
        this.judgeRepository = judgeRepository;
    }

    /* =====================================================
       CORE AUTH HELPER (PUBLIC + CACHED)
       ===================================================== */

    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 🔹 Cache user in Authentication to avoid repeated DB hits
        Object cached = authentication.getDetails();
        if (cached instanceof User user) {
            return user;
        }

        User user = null;
        Object principal = authentication.getPrincipal();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String email = jwt.getClaimAsString("email");
            if (email == null) email = jwt.getClaimAsString("preferred_username");
            if (email == null) email = jwt.getClaimAsString("upn");

            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }
        } else if (principal instanceof UserPrincipal p) {
            user = userRepository.findById(p.userId()).orElse(null);
        } else if (principal instanceof OAuth2User oauthUser) {
            String email = oauthUser.getAttribute("email");
            if (email == null) email = oauthUser.getAttribute("preferred_username");
            if (email == null) email = oauthUser.getAttribute("upn");

            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }
        }

        if (user != null &&
                authentication instanceof org.springframework.security.authentication.AbstractAuthenticationToken token) {
            token.setDetails(user); // safe cache
        }

        return user;
    }



    public boolean isPlatformOwner(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getRole() == Role.OWNER;
    }

    public boolean canAccessUserProfile(Authentication authentication, Long userId) {
        User user = currentUser(authentication);
        return user != null &&
                (user.getRole() == Role.OWNER || user.getUserId().equals(userId));
    }



    public boolean canManageProgram(Authentication authentication, Long programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return programRepository.findById(programId)
                .map(p -> p.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canViewProgram(Authentication authentication, Long programId) {
        return currentUser(authentication) != null;
    }

    public boolean canJudgeAccessProgram(Authentication authentication, Long programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return programRepository.findById(programId)
                .map(p -> p.getJudge() != null &&
                        p.getJudge().getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }



    public boolean isJudgeForActivity(Authentication authentication, Long activityId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return activityRepository.findById(activityId)
                .map(a -> a.getProgram() != null &&
                        a.getProgram().getJudge() != null &&
                        a.getProgram().getJudge().getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canAccessActivityRegistration(Authentication authentication, Long registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return activityRegistrationRepository.findById(registrationId)
                .map(r ->
                        r.getUser().getUserId().equals(user.getUserId()) ||
                                r.getActivity().getProgram().getUser().getUserId().equals(user.getUserId()) ||
                                (r.getActivity().getProgram().getJudge() != null &&
                                        r.getActivity().getProgram().getJudge().getUser().getUserId().equals(user.getUserId()))
                )
                .orElse(false);
    }



    public boolean canAccessJudgeSubmissions(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) return false;

        return user.getRole() == Role.OWNER ||
                judgeRepository.existsByUserUserId(user.getUserId());
    }

    public boolean canVerifySubmission(Authentication authentication, Long submissionId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return submissionRepository.findById(submissionId)
                .map(s -> s.getActivityRegistration()
                        .getActivity()
                        .getProgram()
                        .getJudge()
                        .getUser()
                        .getUserId()
                        .equals(user.getUserId()))
                .orElse(false);
    }



    public boolean canAccessUserWallet(Authentication authentication, Long userId) {
        User user = currentUser(authentication);
        return user != null &&
                (user.getRole() == Role.OWNER || user.getUserId().equals(userId));
    }

    public boolean canAccessProgramWallet(Authentication authentication, UUID programWalletId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return programWalletRepository.findById(programWalletId)
                .map(w -> w.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canAccessProgramWalletsByProgram(Authentication authentication, Long programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return programRepository.findById(programId)
                .map(p ->
                        p.getUser().getUserId().equals(user.getUserId()) ||
                                (p.getJudge() != null &&
                                        p.getJudge().getUser().getUserId().equals(user.getUserId()))
                )
                .orElse(false);
    }
}
