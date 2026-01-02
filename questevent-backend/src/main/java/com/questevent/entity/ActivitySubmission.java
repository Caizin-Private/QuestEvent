package com.questevent.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "activity_submissions")

public class ActivitySubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")

    private Long submissionId;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_submission_registration")
    )
    private ActivityRegistration activityRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_by",
            foreignKey = @ForeignKey(name = "fk_submission_judge")
    )
    private Judge reviewedBy;


    @Column(name = "submission_url", nullable = false)
    private String submissionUrl;
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    @Column(name = "awarded_gems")
    private Integer awardedGems;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @PrePersist
    protected void onSubmit() {
        this.submittedAt = LocalDateTime.now();
    }

}

