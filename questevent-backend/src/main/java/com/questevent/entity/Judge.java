package com.questevent.entity;

import jakarta.persistence.*;

import java.util.List;

public class Judge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "judge_id")
    private Long judgeId;

    @OneToOne
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_judge_user")
    )
    private User user;

    @OneToMany(mappedBy = "reviewedBy")
    private List<ActivitySubmission> reviewedSubmissions;
}
