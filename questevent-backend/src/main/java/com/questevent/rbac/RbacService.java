package com.questevent.rbac;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.Program;
import com.questevent.entity.Activity;
import com.questevent.entity.ActivityRegistration;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.ActivityRegistrationRepository;
import com.questevent.repository.ActivityRepository;
import com.questevent.repository.ActivitySubmissionRepository;
import com.questevent.repository.ProgramRepository;
import com.questevent.repository.ProgramRegistrationRepository;
import com.questevent.repository.ProgramWalletRepository;
import com.questevent.repository.UserRepository;
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

    public RbacService(
            UserRepository userRepository,
            ProgramRepository programRepository,
            ActivityRepository activityRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            ActivityRegistrationRepository activityRegistrationRepository,
            ActivitySubmissionRepository submissionRepository,
            ProgramWalletRepository programWalletRepository
    ) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.activityRepository = activityRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.activityRegistrationRepository = activityRegistrationRepository;
        this.submissionRepository = submissionRepository;
        this.programWalletRepository = programWalletRepository;
    }

    private User currentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {

            Jwt jwt = jwtAuth.getToken();

            String email = jwt.getClaimAsString("email");
            if (email == null) {
                email = jwt.getClaimAsString("preferred_username");
            }
            if (email == null) {
                email = jwt.getClaimAsString("upn");
            }

            if (email == null) return null;

            return userRepository.findByEmail(email).orElse(null);
        }

        // After JwtAuthFilter
        if (principal instanceof UserPrincipal p) {
            return userRepository.findById(p.userId().longValue()).orElse(null);
        }

        // First OAuth2 login
        if (principal instanceof OAuth2User oauthUser) {

            String email = oauthUser.getAttribute("email");
            if (email == null) {
                email = oauthUser.getAttribute("preferred_username");
            }
            if (email == null) {
                email = oauthUser.getAttribute("upn");
            }
            if (email == null) return null;

            return userRepository.findByEmail(email).orElse(null);
        }

        return null;
    }

    public boolean isPlatformOwner(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getRole() == Role.OWNER;
    }

    public boolean canAccessUserProfile(Authentication authentication, Long userId) {

        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return user.getUserId().equals(userId);
    }

    public boolean canManageProgram(Authentication authentication, Long programId) {

        User user = currentUser(authentication);
        if (user == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        if (program.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        return false;
    }

    public boolean canViewProgram(Authentication authentication, Long programId) {
        return currentUser(authentication) != null;
    }

    public boolean canAccessActivityRegistration(Authentication authentication, Long registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        ActivityRegistration registration = activityRegistrationRepository.findById(registrationId).orElse(null);
        if (registration == null) return false;

        // User can access their own registrations
        if (user.getRole() == Role.USER &&
                registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId())) {
            return true;
        }

        // Host can access registrations for their activities
        if (user.getRole() == Role.HOST &&
                registration.getActivity() != null &&
                registration.getActivity().getProgram() != null &&
                registration.getActivity().getProgram().getUser() != null &&
                user.getUserId().equals(registration.getActivity().getProgram().getUser().getUserId())) {
            return true;
        }

        return false;
    }

    public boolean canAccessProgramRegistration(Authentication authentication, Long registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        ProgramRegistration registration = programRegistrationRepository.findById(registrationId).orElse(null);
        if (registration == null) return false;

        // User can access their own registrations
        if (user.getRole() == Role.USER &&
                registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId())) {
            return true;
        }

        // Host can access registrations for their programs
        if (user.getRole() == Role.HOST &&
                registration.getProgram() != null &&
                registration.getProgram().getUser() != null &&
                user.getUserId().equals(registration.getProgram().getUser().getUserId())) {
            return true;
        }

        return false;
    }

    public boolean canVerifySubmission(Authentication authentication, Long submissionId) {

        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        if (user.getRole() == Role.JUDGE) {
            // Controller allows JUDGE to review submissions. There is no judge-program mapping
            // in the current entities, so we can't scope judges to specific programs here.
            return submissionRepository.existsById(submissionId);
        }

        return false;
    }

    public boolean canAccessUserWallet(Authentication authentication, Long userId) {

        User user = currentUser(authentication);
        if (user == null) return false;

        return user.getRole() == Role.OWNER ||
                user.getUserId().equals(userId);
    }

    public boolean canAccessProgramWallet(Authentication authentication, UUID programWalletId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return programWalletRepository.findById(programWalletId)
                .map(wallet -> wallet.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canRegisterForProgram(
            Authentication authentication,
            Long programId,
            Long userId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        // OWNER can do anything
        if (user.getRole() == Role.OWNER) {
            return true;
        }

        // Must match the userId in the request
        if (!user.getUserId().equals(userId)) {
            return false;
        }

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        // Host can register for their own program
        if (user.getRole() == Role.HOST &&
                program.getUser() != null &&
                user.getUserId().equals(program.getUser().getUserId())) {
            // Check duplicate
            return !programRegistrationRepository
                    .existsByProgram_ProgramIdAndUser_UserId(programId, userId);
        }

        // User can register for any program (not their own)
        if (user.getRole() == Role.USER) {
            // Check duplicate
            return !programRegistrationRepository
                    .existsByProgram_ProgramIdAndUser_UserId(programId, userId);
        }

        return false;
    }

    public boolean canRegisterForActivity(
            Authentication authentication,
            Long activityId,
            Long userId
    ) {

        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        if (user.getRole() != Role.USER) {
            return false;
        }

        if (!user.getUserId().equals(userId)) {
            return false;
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) return false;

        Long programId = activity.getProgram() != null ? activity.getProgram().getProgramId() : null;
        if (programId == null) return false;

        // Must be registered to program first
        if (!programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(programId, userId)) {
            return false;
        }

        // Not already registered for activity
        return !activityRegistrationRepository
                .existsByActivity_ActivityIdAndUser_UserId(activityId, userId);
    }

    public boolean canSubmitActivity(
            Authentication authentication,
            Long activityId,
            Long requestUserId
    ) {

        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() != Role.USER) {
            return false;
        }

        if (!user.getUserId().equals(requestUserId)) {
            return false;
        }

        boolean registered =
                activityRegistrationRepository
                        .existsByActivity_ActivityIdAndUser_UserId(activityId, requestUserId);
        if (!registered) {
            return false;
        }

        // We don't have a repo method to fetch registration by (activityId,userId),
        // so mirror current service behavior without changing repositories.
        ActivityRegistration registration = activityRegistrationRepository
                .findAll()
                .stream()
                .filter(r ->
                        r.getActivity() != null &&
                                r.getUser() != null &&
                                r.getActivity().getActivityId().equals(activityId) &&
                                r.getUser().getUserId().equals(requestUserId)
                )
                .findFirst()
                .orElse(null);

        if (registration == null) {
            return false;
        }

        // Not already submitted
        return !submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );
    }
}
