package com.questevent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "judges")
@Data
public class Judge {
    @Id
    @GeneratedValue
    @Column(name = "judge_id", nullable = false, updatable = false)
    private UUID judgeId;

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
