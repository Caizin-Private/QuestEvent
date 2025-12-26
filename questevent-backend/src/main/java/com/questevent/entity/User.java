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
    @Column(name="user_id")
    private Long UserId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;

    private String department;
    private String gender;

    @Column(nullable = false)
    private String role = "USER";


    @ManyToMany
    @JoinTable(
            name = "user_programs",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "program_id")
    )
    private List<Program> programs = new ArrayList<>();


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityParticipation> activitiesParticipated = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<ProgramParticipation> programParticipations = new ArrayList<>();


    @OneToMany(mappedBy = "judgeUser", cascade = CascadeType.ALL)
    private List<ActivityJudge> assignedJudge = new ArrayList<>();

    // Recording action history (PARTICIPATION_HISTORY table)
    @OneToMany(mappedBy = "user")
    private List<ActivityParticipation> participationHistory = new ArrayList<>();
}