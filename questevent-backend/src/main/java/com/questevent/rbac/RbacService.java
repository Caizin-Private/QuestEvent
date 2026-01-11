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
import com.questevent.repository.JudgeRepository;
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

        if (principal instanceof UserPrincipal p) {
            return userRepository.findById(p.userId()).orElse(null);
        }

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

    public boolean canManageProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return program.getUser().getUserId().equals(user.getUserId());
    }

    public boolean canViewProgram(Authentication authentication, UUID programId) {
        return currentUser(authentication) != null;
    }

    public boolean canJudgeAccessProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        return program.getJudge() != null &&
                program.getJudge().getUser().getUserId().equals(user.getUserId());
    }

    public boolean canAccessActivityRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        ActivityRegistration registration = activityRegistrationRepository.findById(registrationId).orElse(null);
        if (registration == null) return false;

        return (registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId()))
                ||
                (registration.getActivity() != null &&
                        registration.getActivity().getProgram() != null &&
                        registration.getActivity().getProgram().getUser() != null &&
                        user.getUserId().equals(
                                registration.getActivity().getProgram().getUser().getUserId()))
                ||
                (registration.getActivity() != null &&
                        registration.getActivity().getProgram() != null &&
                        registration.getActivity().getProgram().getJudge() != null &&
                        user.getUserId().equals(
                                registration.getActivity().getProgram().getJudge().getUser().getUserId()));

    }

    public boolean canAccessProgramRegistration(Authentication authentication, UUID registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        ProgramRegistration registration = programRegistrationRepository.findById(registrationId).orElse(null);
        if (registration == null) return false;

        return (registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId()))
                ||
                (registration.getProgram() != null &&
                        registration.getProgram().getUser() != null &&
                        user.getUserId().equals(
                                registration.getProgram().getUser().getUserId()))
                ||
                (registration.getProgram() != null &&
                        registration.getProgram().getJudge() != null &&
                        user.getUserId().equals(
                                registration.getProgram().getJudge().getUser().getUserId()));

    }

    public boolean canVerifySubmission(Authentication authentication, UUID submissionId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }
        return submissionRepository.findById(submissionId)
                .map(submission -> {
                    ActivityRegistration activityReg = submission.getActivityRegistration();
                    if (activityReg != null && activityReg.getActivity() != null) {
                        Program program = activityReg.getActivity().getProgram();
                        if (program != null && program.getJudge() != null) {
                            return program.getJudge().getUser().getUserId().equals(user.getUserId());
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean isJudgeForProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return programRepository.findById(programId)
                .map(program -> program.getJudge() != null && 
                        program.getJudge().getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean isJudgeForActivity(Authentication authentication, UUID activityId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return activityRepository.findById(activityId)
                .map(activity -> {
                    Program program = activity.getProgram();
                    if (program != null && program.getJudge() != null) {
                        return program.getJudge().getUser().getUserId().equals(user.getUserId());
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean canAccessJudgeSubmissions(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }
        return judgeRepository.findByUserUserId(user.getUserId()).isPresent();
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

    public boolean canAccessMyProgramWallet(
            Authentication authentication,
            UUID programId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Program program = programRepository.findById(programId).orElse(null);
        if (program != null && program.getUser() != null &&
                program.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        return programWalletRepository
                .findByUserUserIdAndProgramProgramId(
                        user.getUserId(),
                        programId
                )
                .isPresent();
    }

    public boolean canAccessProgramWalletsByProgram(
            Authentication authentication,
            UUID programId
    ) {
        User user = currentUser(authentication);
        if (user == null) {
            return false;
        }

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null || program.getUser() == null) {
            return false;
        }

        return program.getUser().getUserId().equals(user.getUserId())
                ||
                (program.getJudge() != null &&
                        program.getJudge().getUser().getUserId().equals(user.getUserId()));

    }

    public boolean canRegisterForProgram(
            Authentication authentication,
            UUID programId,
            Long userId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        if (program.getUser() != null &&
                user.getUserId().equals(program.getUser().getUserId())) {
            return !programRegistrationRepository
                    .existsByProgram_ProgramIdAndUser_UserId(programId, userId);
        }

        if (!user.getUserId().equals(userId)) {
            return false;
        }

        return !programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(programId, userId);
    }

    public boolean canRegisterForActivity(
            Authentication authentication,
            UUID activityId,
            Long userId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) return false;

        UUID programId = activity.getProgram() != null ? activity.getProgram().getProgramId() : null;
        if (programId == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        if (program.getUser() != null && user.getUserId().equals(program.getUser().getUserId())) {
            if (!programRegistrationRepository.existsByProgram_ProgramIdAndUser_UserId(programId, userId)) {
                return false;
            }
            return !activityRegistrationRepository.existsByActivity_ActivityIdAndUser_UserId(activityId, userId);
        }

        if (!user.getUserId().equals(userId)) {
            return false;
        }

        if (!programRegistrationRepository.existsByProgram_ProgramIdAndUser_UserId(programId, userId)) {
            return false;
        }
        return !activityRegistrationRepository.existsByActivity_ActivityIdAndUser_UserId(activityId, userId);
    }

    public boolean canSubmitActivity(
            Authentication authentication,
            UUID activityId,
            Long requestUserId
    ) {
        User user = currentUser(authentication);
        if (user == null) {
            return false;
        }

        // Only USER role can submit
        if (user.getRole() != Role.USER) {
            return false;
        }

        // User can submit only for self
        if (!user.getUserId().equals(requestUserId)) {
            return false;
        }

        // Must be registered for the activity
        ActivityRegistration registration =
                activityRegistrationRepository
                        .findByActivityActivityIdAndUserUserId(
                                activityId,
                                requestUserId
                        )
                        .orElse(null);

        if (registration == null) {
            return false;
        }

        // Submission must not already exist
        return !submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        registration.getActivityRegistrationId()
                );
    }

    public UUID getProgramIdByActivityId(UUID activityId) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null && activity.getProgram() != null) {
            return activity.getProgram().getProgramId();
        }
        return null;
    }
}