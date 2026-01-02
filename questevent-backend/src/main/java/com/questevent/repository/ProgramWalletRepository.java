package com.questevent.repository;

import com.questevent.entity.Program;
import com.questevent.entity.ProgramWallet;
import com.questevent.entity.User;
import com.questevent.enums.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramWalletRepository extends JpaRepository<ProgramWallet, UUID> {

    Optional<ProgramWallet> findByUserAndProgram(User user, Program program);

    Optional<ProgramWallet> findByUserUserIdAndProgramProgramId(
            Long userId,
            Long programId
    );

    List<ProgramWallet> findByUser(User user);

    List<ProgramWallet> findByProgramProgramId(Long programId);

    List<ProgramWallet> findByProgramStatusAndProgramEndDateBefore(ProgramStatus status, LocalDateTime now);
}
