package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "wallets")
public class UserWallet {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    @OneToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name="gems")
    private int gems;

}
