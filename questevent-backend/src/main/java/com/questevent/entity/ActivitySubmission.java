package com.questevent.entity;

import com.questevent.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "activity_submissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_submission_registration",
                        columnNames = "activity_registration_id"
                )
        }
)
public class ActivitySubmission {
    @Id
    @GeneratedValue
    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "activity_registration_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_submission_registration")
    )
    private ActivityRegistration activityRegistration;


    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;


    @Column(name = "submission_url", nullable = false)
    private String submissionUrl;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "awarded_gems")
    private Long awardedGems;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "reviewed_by",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_submission_judge")
    )
    private Judge reviewedBy;

    @PrePersist
    protected void onSubmit() {
        this.submittedAt = Instant.now();
        this.reviewStatus = ReviewStatus.PENDING;
    }

}

