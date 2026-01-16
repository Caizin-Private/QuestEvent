package com.questevent.rbac;

import com.questevent.entity.*;
import com.questevent.enums.Role;
import com.questevent.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return null;
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getClaimAsString("upn");
        if (email == null) return null;

        return userRepository.findByEmail(email).orElse(null);
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

    public boolean canAccessUserWallet(Authentication authentication, Long userId) {
        return canAccessUserProfile(authentication, userId);
    }

    public boolean canManageProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null || program.getUser() == null) return false;

        return user.getRole() == Role.OWNER ||
                program.getUser().getUserId().equals(user.getUserId());
    }

    public boolean canViewProgram(Authentication authentication, UUID programId) {
        return currentUser(authentication) != null;
    }

    public boolean canJudgeAccessProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        Program program = programRepository.findById(programId).orElse(null);
        return program != null &&
                program.getJudge() != null &&
                program.getJudge().getUser().getUserId().equals(user.getUserId());
    }

    public boolean canAccessProgramRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        ProgramRegistration reg =
                programRegistrationRepository.findById(registrationId).orElse(null);
        if (reg == null) return false;

        if (reg.getUser() != null &&
                reg.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        Program program = reg.getProgram();
        if (program == null) return false;

        if (program.getUser() != null &&
                program.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        return program.getJudge() != null &&
                program.getJudge().getUser().getUserId().equals(user.getUserId());
    }

    public boolean canAccessActivityRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        ActivityRegistration reg =
                activityRegistrationRepository.findById(registrationId).orElse(null);
        if (reg == null) return false;

        if (reg.getUser() != null &&
                reg.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        Activity activity = reg.getActivity();
        if (activity == null || activity.getProgram() == null) return false;

        Program program = activity.getProgram();

        if (program.getUser() != null &&
                program.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        return program.getJudge() != null &&
                program.getJudge().getUser().getUserId().equals(user.getUserId());
    }

    public boolean canVerifySubmission(Authentication authentication, UUID submissionId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) return true;

        return submissionRepository.findById(submissionId)
                .map(sub -> {
                    ActivityRegistration reg = sub.getActivityRegistration();
                    if (reg == null || reg.getActivity() == null) return false;

                    Program program = reg.getActivity().getProgram();
                    return program != null &&
                            program.getJudge() != null &&
                            program.getJudge().getUser().getUserId().equals(user.getUserId());
                })
                .orElse(false);
    }

    public boolean canSubmitActivity(
            Authentication authentication,
            UUID activityId,
            Long userId
    ) {
        User user = currentUser(authentication);
        if (user == null || user.getRole() != Role.USER) return false;
        if (!user.getUserId().equals(userId)) return false;

        ActivityRegistration reg =
                activityRegistrationRepository
                        .findByActivityActivityIdAndUserUserId(activityId, userId)
                        .orElse(null);

        return reg != null &&
                !submissionRepository.existsByActivityRegistration_ActivityRegistrationId(
                        reg.getActivityRegistrationId());
    }

    public UUID getProgramIdByActivityId(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(a -> a.getProgram().getProgramId())
                .orElse(null);
    }
}
