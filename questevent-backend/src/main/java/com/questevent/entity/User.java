package com.questevent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "wallet_id", referencedColumnName = "walletId")
    private Wallet wallet;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String role;

    @ManyToMany
    @JoinTable(
            name = "user_programs",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "program_id")
    )
    private List<Program> programs = new ArrayList<>();

    @OneToMany(mappedBy = "activity")
    private List<ActivityRegistration> activitiesRegistration = new ArrayList<>();

    @OneToMany(mappedBy = "program")
    private List<ProgramRegistration> programsRegistration = new ArrayList<>();

    @Column(name = "is_organizer", nullable = false)
    private boolean isOrganizer;

}