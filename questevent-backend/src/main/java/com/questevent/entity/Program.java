package com.questevent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.enums.Department;
import com.questevent.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.Data;

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

    @Column(name = "department")
    private Department dept;

    @Column(name = "startDate")
    private LocalDateTime startDate;

    @Column(name = "endDate")
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer registrationFee;

    @Enumerated(EnumType.STRING)
    private ProgramStatus status = ProgramStatus.DRAFT;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramWallet> programWallets;
}

