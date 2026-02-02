package com.questevent.rbac;

import com.questevent.entity.*;
import com.questevent.enums.ReviewStatus;
import com.questevent.enums.Role;
import com.questevent.repository.*;
import org.springframework.security.core.Authentication;
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
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }

        Jwt jwt = jwtAuth.getToken();

        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getClaimAsString("upn");
        if (email == null) email = jwt.getClaimAsString("email");
        if (email == null) return null;

        return userRepository.findByEmail(email).orElse(null);
    }

    private boolean isOwner(User user) {
        return user != null && user.getRole() == Role.OWNER;
    }

    public boolean isPlatformOwner(Authentication authentication) {
        return isOwner(currentUser(authentication));
    }

    public boolean canAccessUserProfile(Authentication authentication, Long userId) {
        User user = currentUser(authentication);
        return user != null &&
                (isOwner(user) || user.getUserId().equals(userId));
    }

    public boolean canManageProgram(Authentication authentication, UUID programId) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return programRepository.findById(programId)
                .map(p -> p.getUser() != null &&
                        p.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canViewProgram(Authentication authentication, UUID programId) {
        return currentUser(authentication) != null;
    }

    public boolean canJudgeAccessAnyProgram(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return !programRepository
                .findByJudgeUserId(user.getUserId())
                .isEmpty();
    }

    public boolean canJudgeAccessProgram(
            Authentication authentication,
            UUID programId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return programRepository.findById(programId)
                .map(p ->
                        p.getJudge() != null &&
                                p.getJudge().getUser().getUserId().equals(user.getUserId())
                )
                .orElse(false);
    }

    public UUID getProgramIdByActivityId(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(a -> a.getProgram().getProgramId())
                .orElse(null);
    }

    public boolean canRegisterForProgram(
            Authentication authentication,
            UUID programId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        Program program = programRepository.findById(programId).orElse(null);
        if (program == null) return false;

        // ❌ Judge of this program cannot register for it
        Judge judge = program.getJudge();
        if (judge != null &&
                judge.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        return true;
    }


    public boolean canAccessProgramRegistration(
            Authentication authentication,
            UUID registrationId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return programRegistrationRepository.findById(registrationId)
                .map(reg ->
                        reg.getUser().getUserId().equals(user.getUserId()) ||
                                reg.getProgram().getUser().getUserId().equals(user.getUserId()) ||
                                (reg.getProgram().getJudge() != null &&
                                        reg.getProgram().getJudge().getUser().getUserId().equals(user.getUserId()))
                )
                .orElse(false);
    }

    public boolean canRegisterForActivity(
            Authentication authentication,
            UUID activityId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        Activity activity =
                activityRepository.findById(activityId).orElse(null);
        if (activity == null) return false;

        Program program = activity.getProgram();
        if (program == null) return false;

        // ❌ Judge of this program cannot register for its activities
        Judge judge = program.getJudge();
        if (judge != null &&
                judge.getUser().getUserId().equals(user.getUserId())) {
            return false;
        }

        // User must be registered for the program first
        boolean isRegisteredForProgram = programRegistrationRepository
                .existsByProgram_ProgramIdAndUser_UserId(
                        program.getProgramId(),
                        user.getUserId()
                );
        if (!isRegisteredForProgram) {
            return false;
        }

        // User must not already be registered for this activity
        return activityRegistrationRepository
                .findByActivityActivityIdAndUserUserId(
                        activityId,
                        user.getUserId()
                )
                .isEmpty();
    }


    public boolean canAccessActivityRegistration(
            Authentication authentication,
            UUID registrationId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return activityRegistrationRepository.findById(registrationId)
                .map(reg ->
                        reg.getUser().getUserId().equals(user.getUserId()) ||
                                reg.getActivity()
                                        .getProgram()
                                        .getUser()
                                        .getUserId()
                                        .equals(user.getUserId()) ||
                                (
                                        reg.getActivity().getProgram().getJudge() != null &&
                                                reg.getActivity()
                                                        .getProgram()
                                                        .getJudge()
                                                        .getUser()
                                                        .getUserId()
                                                        .equals(user.getUserId())
                                )
                )
                .orElse(false);
    }

    public boolean canSubmitActivity(
            Authentication authentication,
            UUID activityId)
    {
        User user = currentUser(authentication);
        if (user == null) return false;

        ActivityRegistration reg =
                activityRegistrationRepository
                        .findByActivityActivityIdAndUserUserId(activityId, user.getUserId())
                        .orElse(null);

        if (reg == null) return false;

        return !submissionRepository
                .existsByActivityRegistration_ActivityRegistrationId(
                        reg.getActivityRegistrationId());
    }

    public boolean canViewOwnSubmissionByActivity(
            Authentication authentication,
            UUID activityId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        return submissionRepository
                .findByActivityRegistrationActivityActivityIdAndActivityRegistrationUserUserId(
                        activityId,
                        user.getUserId()
                )
                .isPresent();
    }

    public boolean canResubmitSubmission(
            Authentication authentication,
            UUID activityId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;

        return submissionRepository
                .findByActivityRegistrationActivityActivityIdAndActivityRegistrationUserUserId(
                        activityId,
                        user.getUserId()
                )
                .map(submission ->
                        submission.getReviewStatus() == ReviewStatus.REJECTED
                )
                .orElse(false);
    }

    public boolean canJudgeAccessSubmission(
            Authentication authentication,
            UUID submissionId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return submissionRepository.findById(submissionId)
                .map(sub -> {
                    Program program =
                            sub.getActivityRegistration()
                                    .getActivity()
                                    .getProgram();

                    Judge judge = program.getJudge();
                    return judge != null &&
                            judge.getUser().getUserId().equals(user.getUserId());
                })
                .orElse(false);
    }

    public boolean canAccessProgramWallet(
            Authentication authentication,
            UUID programWalletId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return programWalletRepository.findById(programWalletId)
                .map(w ->
                        w.getUser().getUserId().equals(user.getUserId()) ||
                                w.getProgram().getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }

    public boolean canAccessMyProgramWallet(
            Authentication authentication,
            UUID programId
    ) {
        return currentUser(authentication) != null;
    }

    public boolean canAccessProgramWalletsByProgram(
            Authentication authentication,
            UUID programId
    ) {
        User user = currentUser(authentication);
        if (user == null) return false;
        if (isOwner(user)) return true;

        return programRepository.findById(programId)
                .map(p -> p.getUser().getUserId().equals(user.getUserId()))
                .orElse(false);
    }
}

