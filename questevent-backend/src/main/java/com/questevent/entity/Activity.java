package com.questevent.entity;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "program_id")
    )
    private Program program;


    @Column(name = "name", nullable = false, length = 200)
    private String activityName;

    @Column(name = "activity_duration")
    private Integer activityDuration;

    @Column(name = "rulebook", columnDefinition = "TEXT")
    private String acitivityRulebook;

    @Column(name = "description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "reward_gems")
    private Integer reward_gems;

    @Column(name = "creationTime")
    private LocalDateTime activityCreatedAt;

}