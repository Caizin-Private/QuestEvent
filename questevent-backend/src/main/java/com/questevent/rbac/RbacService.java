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

    public boolean canManageProgram(Authentication authentication, Long programId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        return program.getUser().getUserId().equals(user.getUserId());
    }

    public boolean canViewProgram(Authentication authentication, Long programId) {
        return currentUser(authentication) != null;
    }

    public boolean canJudgeAccessProgram(Authentication authentication, Long programId) {
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

    public boolean canAccessActivityRegistration(Authentication authentication, Long registrationId) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        ActivityRegistration registration = activityRegistrationRepository.findById(registrationId).orElse(null);
        if (registration == null) return false;

        if (registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId())) {
            return true;
        }

        if (registration.getActivity() != null &&
                registration.getActivity().getProgram() != null &&
                registration.getActivity().getProgram().getUser() != null &&
                user.getUserId().equals(registration.getActivity().getProgram().getUser().getUserId())) {
            return true;
        }

        if (registration.getActivity() != null &&
                registration.getActivity().getProgram() != null &&
                registration.getActivity().getProgram().getJudge() != null &&
                user.getUserId().equals(registration.getActivity().getProgram().getJudge().getUser().getUserId())) {
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

        if (registration.getUser() != null &&
                user.getUserId().equals(registration.getUser().getUserId())) {
            return true;
        }

        if (registration.getProgram() != null &&
                registration.getProgram().getUser() != null &&
                user.getUserId().equals(registration.getProgram().getUser().getUserId())) {
            return true;
        }

        if (registration.getProgram() != null &&
                registration.getProgram().getJudge() != null &&
                user.getUserId().equals(registration.getProgram().getJudge().getUser().getUserId())) {
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

    public boolean isJudgeForProgram(Authentication authentication, Long programId) {
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

    public boolean isJudgeForActivity(Authentication authentication, Long activityId) {
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
            Long programId
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
            Long programId
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

        if (program.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        if (program.getJudge() != null &&
                program.getJudge().getUser().getUserId().equals(user.getUserId())) {
            return true;
        }

        return false;
    }

    public boolean canRegisterForProgram(
            Authentication authentication,
            Long programId,
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
            Long activityId,
            Long userId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        if (user.getRole() == Role.OWNER) {
            return true;
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) return false;

        Long programId = activity.getProgram() != null ? activity.getProgram().getProgramId() : null;
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

        if (!activityRegistrationRepository.existsByActivity_ActivityIdAndUser_UserId(activityId, requestUserId)) {
            return false;
        }

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

        return !submissionRepository.existsByActivityRegistration_ActivityRegistrationId(
                registration.getActivityRegistrationId()
        );
    }

    public Long getProgramIdByActivityId(Long activityId) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null && activity.getProgram() != null) {
            return activity.getProgram().getProgramId();
        }
        return null;
    }
}