package com.questevent.entity;
 
import com.questevent.enums.CompletionStatus;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Id;
 
 
@Entity
@Table(name = "activity_registrations",
uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id","user_id"}))
@Data
public class ActivityRegistration {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_registration_id")
    private long activityRegistrationId;

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
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false)
    private CompletionStatus completionStatus;

 
}
 