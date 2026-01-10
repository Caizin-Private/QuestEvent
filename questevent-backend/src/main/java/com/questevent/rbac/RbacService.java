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


    public RbacService(
            UserRepository userRepository,
            ProgramRepository programRepository,
            ActivityRepository activityRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            ActivityRegistrationRepository activityRegistrationRepository,
            ActivitySubmissionRepository submissionRepository
    ) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.activityRepository = activityRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.activityRegistrationRepository = activityRegistrationRepository;
        this.submissionRepository = submissionRepository;

    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return resolveUserByEmail(extractEmail(jwtAuth.getToken()));
        }

        if (principal instanceof UserPrincipal p) {
            return userRepository.findById(p.userId()).orElse(null);
        }

        if (principal instanceof OAuth2User oauth) {
            return resolveUserByEmail(extractEmail(oauth));
        }

        return null;
    }

    private User resolveUserByEmail(String email) {
        return email == null ? null : userRepository.findByEmail(email).orElse(null);
    }

    private String extractEmail(Jwt jwt) {
        return firstNonNull(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("upn")
        );
    }

    private String extractEmail(OAuth2User oauth) {
        return firstNonNull(
                oauth.getAttribute("email"),
                oauth.getAttribute("preferred_username"),
                oauth.getAttribute("upn")
        );
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public boolean isPlatformOwner(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getRole() == Role.OWNER;
    }

    public boolean canViewProgram(Authentication authentication) {
        return currentUser(authentication) != null;
    }

    public boolean canManageProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) return true;

        return programRepository.findById(programId)
                .map(p -> p.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canJudgeAccessProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) return true;

        return programRepository.findById(programId)
                .map(p -> p.getJudge() != null &&
                        p.getJudge().getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canAccessActivityRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) return true;

        return activityRegistrationRepository.findById(registrationId)
                .map(reg ->
                        isSameUser(reg.getUser(), user) ||
                                isProgramHost(reg.getActivity(), user) ||
                                isProgramJudge(reg.getActivity(), user))
                .orElse(false);
    }

    public boolean canAccessProgramRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) return true;

        return programRegistrationRepository.findById(registrationId)
                .map(reg ->
                        isSameUser(reg.getUser(), user) ||
                                isSameUser(reg.getProgram().getUser(), user) ||
                                isSameUser(reg.getProgram().getJudge().getUser(), user))
                .orElse(false);
    }

    public boolean canVerifySubmission(Authentication authentication, UUID submissionId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) return true;

        return submissionRepository.findById(submissionId)
                .map(sub ->
                        sub.getActivityRegistration() != null &&
                                sub.getActivityRegistration().getActivity() != null &&
                                sub.getActivityRegistration().getActivity().getProgram() != null &&
                                sub.getActivityRegistration().getActivity().getProgram().getJudge() != null &&
                                isSameUser(
                                        sub.getActivityRegistration()
                                                .getActivity()
                                                .getProgram()
                                                .getJudge()
                                                .getUser(),
                                        user))
                .orElse(false);
    }

    private boolean isSameUser(User entityUser, User current) {
        return entityUser != null &&
                entityUser.getUserId().equals(current.getUserId());
    }

    private boolean isProgramHost(Activity activity, User user) {
        return activity != null &&
                activity.getProgram() != null &&
                isSameUser(activity.getProgram().getUser(), user);
    }

    private boolean isProgramJudge(Activity activity, User user) {
        return activity != null &&
                activity.getProgram() != null &&
                activity.getProgram().getJudge() != null &&
                isSameUser(activity.getProgram().getJudge().getUser(), user);
    }

    public UUID getProgramIdByActivityId(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(a -> a.getProgram() != null ? a.getProgram().getProgramId() : null)
                .orElse(null);
    }
}
