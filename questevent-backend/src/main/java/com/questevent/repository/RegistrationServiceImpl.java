package com.questevent.repository;

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

    public RegistrationServiceImpl(
            ProgramRepository programRepository,
            ActivityRepository activityRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            ActivityRegistrationRepository activityRegistrationRepository,
            WalletService walletService
    ) {
        this.programRepository = programRepository;
        this.activityRepository = activityRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.activityRegistrationRepository = activityRegistrationRepository;
        this.walletService = walletService;
    }

    @Override
    public void registerForProgram(Long programId, Long userId) {

    }

    @Override
    public void registerForActivity(Long activityId, Long userId) {

    }
}
