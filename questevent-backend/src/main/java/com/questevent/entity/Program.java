package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cascade;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long programId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String programTitle;

    private String programDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Department department;

    @Column(name = "startDate")
    private Instant startDate;

    @Column(name = "endDate")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    private ProgramStatus status;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramWallet> programWallets;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activity> activities;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramRegistration> programRegistrations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "judge_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_program_judge")
    )
    @Cascade(org.hibernate.annotations.CascadeType.PERSIST)
    private Judge judge;

}

