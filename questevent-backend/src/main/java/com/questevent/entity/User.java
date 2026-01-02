package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.enums.Department;
import com.questevent.enums.Role;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Program> hostedPrograms;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRegistration> activityRegistrations;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramRegistration> programRegistrations;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ProgramWallet> programWallets;

}