package com.questevent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "judges")
@Data
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

    @OneToMany(mappedBy = "judge")
    private List<Program> programs;

    @OneToMany(mappedBy = "reviewedBy")
    private List<ActivitySubmission> reviewedSubmissions;

}
