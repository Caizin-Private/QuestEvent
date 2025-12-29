package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(
        name = "program_wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_program",
                        columnNames = {"user_id", "program_id"}
                )
        }
)
public class ProgramWallet {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "program_wallet_id", updatable = false, nullable = false)
    private UUID programWalletId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "gems", nullable = false)
    private int gems;
}
