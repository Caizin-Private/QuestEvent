package com.questevent.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer registrationFee;

    @Enumerated(EnumType.STRING)
    private ProgramStatus status = ProgramStatus.DRAFT;

    // GETTERS & SETTERS
}
