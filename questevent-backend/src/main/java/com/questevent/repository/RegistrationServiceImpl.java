package com.questevent.repository;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramRegistration;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import com.questevent.service.RegistrationService;
import com.questevent.service.WalletService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final ProgramRepository programRepository;
    private final ActivityRepository activityRepository;
    private final ProgramRegistrationRepository programRegistrationRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;

    public RegistrationServiceImpl(
            ProgramRepository programRepository,
            ActivityRepository activityRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            ActivityRegistrationRepository activityRegistrationRepository,
            WalletService walletService,
            UserRepository userRepository
    ) {
        this.programRepository = programRepository;
        this.activityRepository = activityRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.activityRegistrationRepository = activityRegistrationRepository;
        this.walletService = walletService;
        this.userRepository = userRepository;

    }

    public void registerForProgram(Long programId, Long userId) {


        boolean alreadyRegistered =
                programRegistrationRepository
                        .existsByProgram_IdAndUser_Id(programId, userId);

        if (alreadyRegistered) {
            throw new RuntimeException("User already registered for this program");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (program.getStatus()!= ProgramStatus.ACTIVE) {
            throw new RuntimeException("Program is not active");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProgramRegistration registration = new ProgramRegistration();
        registration.setProgram(program);
        registration.setUser(user);

        programRegistrationRepository.save(registration);
    }

    @Override
    public void registerForActivity(Long activityId, Long userId) {

    }
}
