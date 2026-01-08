package com.questevent.entity;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

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
    private String activityRulebook;

    @Column(name = "description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "reward_gems",nullable = false)
    private Long rewardGems;

    @Column(name = "created_at",updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    @Column(name = "is_compulsory", nullable = false)
    private Boolean isCompulsory;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRegistration> registrations;
}