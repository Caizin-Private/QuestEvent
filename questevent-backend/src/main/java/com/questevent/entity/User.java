package com.questevent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserWallet wallet;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "user")
    private List<Program> hostedPrograms;

    @OneToMany(mappedBy = "user")
    private List<ActivityRegistration> activityRegistrations;

    @OneToMany(mappedBy = "user")
    private List<ProgramRegistration> programRegistrations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ProgramWallet> programWallets;

}