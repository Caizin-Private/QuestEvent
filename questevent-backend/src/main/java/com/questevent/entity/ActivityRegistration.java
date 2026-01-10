package com.questevent.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.questevent.enums.CompletionStatus;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;


import java.util.UUID;


@Entity
@Table(name = "activity_registrations",
uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id","user_id"}))
@Data
public class ActivityRegistration {

    @Id
    @GeneratedValue
    @Column(name = "activity_registration_id", nullable = false, updatable = false)
    private UUID activityRegistrationId;

    @ManyToOne
    @JoinColumn(
            name = "activity_id",
            foreignKey = @ForeignKey(name = "activity_id")
    )
    private Activity activity;

    @ManyToOne
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "user_id")
    )
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false)
    private CompletionStatus completionStatus;

    @OneToOne(mappedBy = "activityRegistration", cascade = CascadeType.ALL, orphanRemoval = true)
    private ActivitySubmission activitySubmission;

 
}
 